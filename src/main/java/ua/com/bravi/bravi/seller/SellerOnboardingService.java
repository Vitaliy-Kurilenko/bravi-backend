package ua.com.bravi.bravi.seller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.AccountView;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.api.IdentityApi;
import ua.com.bravi.bravi.seller.account.api.SellerAccountView;
import ua.com.bravi.bravi.seller.account.api.SellerAccountsApi;
import ua.com.bravi.bravi.seller.component.SellerAccountResolver;
import ua.com.bravi.bravi.seller.channels.api.SalesChannelsApi;
import ua.com.bravi.bravi.seller.controller.dto.in.LogoUploadUrlRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.LogoUploadUrlResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.OnboardingStateResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.OnboardingStateResponse.OnboardingStepsResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreContactResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.controller.mapper.StoreContactDtoMapper;
import ua.com.bravi.bravi.seller.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.seller.controller.mapper.StoreLogoDtoMapper;
import ua.com.bravi.bravi.seller.exception.EmailNotVerifiedException;
import ua.com.bravi.bravi.seller.exception.OnboardingIncompleteException;
import ua.com.bravi.bravi.seller.exception.StoreAlreadyExistsException;
import ua.com.bravi.bravi.seller.stores.api.StoreDraft;
import ua.com.bravi.bravi.seller.stores.api.StoreSettings;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.seller.stores.contacts.api.StoreContactsApi;
import ua.com.bravi.bravi.seller.stores.contacts.domain.StoreContact;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates seller onboarding (spec §5) under {@code /seller/onboarding}. Resolves the account
 * from the current authorization context ({@code X-Account-Id}), drives the DRAFT store / settings /
 * contacts steps, and finalizes onboarding (account ACTIVE, onboarding COMPLETED, store ACTIVE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOnboardingService {

    private static final String ONBOARDING_IN_PROGRESS = "IN_PROGRESS";
    private static final String ONBOARDING_COMPLETED = "COMPLETED";

    private final AccessApi accessApi;
    private final SellerAccountResolver sellerAccountResolver;
    private final IdentityApi identityApi;
    private final StoresApi storesApi;
    private final StoreContactsApi storeContactsApi;
    private final SalesChannelsApi salesChannelsApi;
    private final SellerAccountsApi sellerAccountsApi;
    private final InvocationContext invocationContext;
    private final StoreDtoMapper storeDtoMapper;
    private final StoreLogoDtoMapper storeLogoDtoMapper;
    private final StoreContactDtoMapper storeContactDtoMapper;

    @Transactional(readOnly = true)
    public OnboardingStateResponse getState() {
        return buildState(resolveAccountId());
    }

    @Transactional
    public StoreResponse createStore(StoreDraft draft) {
        Long accountId = resolveAccountId();
        if (storesApi.findFirstStoreIdByAccountId(accountId).isPresent()) {
            throw new StoreAlreadyExistsException("This seller account already has a store");
        }
        StoreView view = storesApi.createDraftStore(accountId, draft);
        salesChannelsApi.createManualChannel(view.id());
        sellerAccountsApi.updateOnboardingStatus(accountId, ONBOARDING_IN_PROGRESS);
        log.info("Onboarding started accountId={} storeId={}", accountId, view.id());
        return storeDtoMapper.toResponse(view);
    }

    @Transactional
    public void updateStore(StoreDraft draft, String logoStorageKey) {
        Long storeId = requireStoreId(resolveAccountId());
        storesApi.updateDraftStore(storeId, draft);
        if (logoStorageKey != null) {
            storesApi.confirmLogo(storeId, logoStorageKey);
        }
    }

    @Transactional(readOnly = true)
    public LogoUploadUrlResponse presignLogoUpload(LogoUploadUrlRequest request) {
        Long storeId = requireStoreId(resolveAccountId());
        return storeLogoDtoMapper.toResponse(
                storesApi.presignLogoUpload(storeId, storeLogoDtoMapper.toUpload(request)));
    }

    @Transactional
    public StoreResponse removeLogo() {
        Long storeId = requireStoreId(resolveAccountId());
        return storeDtoMapper.toResponse(storesApi.removeLogo(storeId));
    }

    @Transactional
    public void updateSettings(StoreSettings patch) {
        storesApi.updateSettings(requireStoreId(resolveAccountId()), patch);
    }

    @Transactional
    public List<StoreContactResponse> replaceContacts(List<StoreContact> contacts) {
        return storeContactDtoMapper.toResponses(
                storeContactsApi.replaceContacts(requireStoreId(resolveAccountId()), contacts));
    }

    @Transactional
    public OnboardingStateResponse complete() {
        Long accountId = resolveAccountId();
        requireEmailVerified();

        List<String> missing = new ArrayList<>();
        Optional<Long> storeId = storesApi.findFirstStoreIdByAccountId(accountId);
        if (storeId.isEmpty()) {
            missing.add("store");
        } else {
            StoreView store = storesApi.getStoreById(storeId.get()).orElseThrow();
            if (store.name() == null || store.name().isBlank()) {
                missing.add("store_name");
            }
            if (!salesChannelsApi.hasManualChannel(storeId.get())) {
                missing.add("sales_channel");
            }
        }
        if (!missing.isEmpty()) {
            log.warn("Onboarding completion rejected accountId={} missing={}", accountId, missing);
            throw new OnboardingIncompleteException(missing);
        }

        accessApi.activateAccount(accountId);
        sellerAccountsApi.updateOnboardingStatus(accountId, ONBOARDING_COMPLETED);
        storesApi.activateStore(storeId.get());

        log.info("Onboarding completed accountId={} storeId={}", accountId, storeId.get());
        return buildState(accountId);
    }

    private OnboardingStateResponse buildState(Long accountId) {
        AccountView account = accessApi.findAccountById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        String onboardingStatus = sellerAccountsApi.findByAccountId(accountId)
                .map(SellerAccountView::onboardingStatus)
                .orElse(null);

        Optional<StoreView> store = storesApi.findFirstStoreIdByAccountId(accountId)
                .flatMap(storesApi::getStoreById);

        boolean storeStep = store.map(s -> s.name() != null && !s.name().isBlank()).orElse(false);
        boolean settingsStep = store.isPresent();
        boolean contactsStep = store
                .map(s -> !storeContactsApi.findByStoreId(s.id()).isEmpty())
                .orElse(false);

        return new OnboardingStateResponse(
                account.status(),
                onboardingStatus,
                new OnboardingStepsResponse(storeStep, settingsStep, contactsStep),
                store.map(storeDtoMapper::toResponse).orElse(null));
    }

    /**
     * Returns the account resolved by the account-context interceptor from {@code X-Account-Id}
     * (validated ACTIVE membership); 403 when no context or the account is not a seller account.
     */
    private Long resolveAccountId() {
        return sellerAccountResolver.resolveSellerAccountId();
    }

    private Long requireStoreId(Long accountId) {
        return storesApi.findFirstStoreIdByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Store not found — create it first"));
    }

    private void requireEmailVerified() {
        Long userId = invocationContext.getUserId();
        CurrentUserView user = identityApi.getById(userId);
        if (!user.emailVerified() && invocationContext.isTokenEmailVerified()) {
            user = identityApi.syncEmailVerified(userId, true);
        }
        if (!user.emailVerified()) {
            log.warn("Onboarding completion rejected userId={} reason=email_not_verified", userId);
            throw new EmailNotVerifiedException("Verify your email before completing onboarding");
        }
    }
}

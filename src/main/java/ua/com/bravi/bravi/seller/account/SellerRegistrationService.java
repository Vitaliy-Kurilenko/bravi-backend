package ua.com.bravi.bravi.seller.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.OwnerAccountView;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.api.IdentityApi;
import ua.com.bravi.bravi.seller.account.api.SellerRegistrationApi;
import ua.com.bravi.bravi.seller.account.api.SellerRegistrationCommand;
import ua.com.bravi.bravi.seller.account.api.SellerRegistrationView;
import ua.com.bravi.bravi.seller.account.domain.SellerOnboardingStatus;
import ua.com.bravi.bravi.seller.account.exception.RegistrationContextConflictException;
import ua.com.bravi.bravi.seller.account.persistence.ISellerAccountRepository;
import ua.com.bravi.bravi.seller.account.persistence.entity.SellerAccountEntity;

/**
 * Orchestrates seller registration invoked by the Auth Service: creates (or returns, idempotently)
 * the User + SELLER Account + SellerAccount profile + owner Membership in one transaction.
 */
@Service
@RequiredArgsConstructor
public class SellerRegistrationService implements SellerRegistrationApi {

    private static final String SELLER = "SELLER";

    private final IdentityApi identityApi;
    private final AccessApi accessApi;
    private final ISellerAccountRepository sellerAccountRepository;

    @Override
    @Transactional
    public SellerRegistrationView register(SellerRegistrationCommand command) {
        // Same email under a different Keycloak identity => inconsistent registration context.
        identityApi.findByEmail(command.email()).ifPresent(existing -> {
            if (!command.keycloakUserId().equals(existing.extId())) {
                throw new RegistrationContextConflictException(
                        "Email already registered for a different identity");
            }
        });

        CurrentUserView user = identityApi.provisionUser(
                command.keycloakUserId(), command.email(), command.firstName(), command.lastName());

        OwnerAccountView owner = accessApi.findOwnerAccount(user.id(), SELLER)
                .orElseGet(() -> provisionSellerContext(user.id(), command.email()));

        // Ensure the seller profile exists (covers a partial prior state).
        if (!sellerAccountRepository.existsById(owner.accountId())) {
            saveSellerProfile(owner.accountId(), command.email());
        }
        String onboardingStatus = sellerAccountRepository.findById(owner.accountId())
                .map(entity -> entity.getOnboardingStatus().name())
                .orElse(SellerOnboardingStatus.NOT_STARTED.name());

        return new SellerRegistrationView(
                user.publicId(),
                owner.accountPublicId(),
                owner.membershipPublicId(),
                owner.accountType(),
                owner.accountStatus(),
                onboardingStatus);
    }

    private OwnerAccountView provisionSellerContext(Long userId, String contactEmail) {
        OwnerAccountView owner = accessApi.provisionOwnerAccount(userId, SELLER);
        saveSellerProfile(owner.accountId(), contactEmail);
        return owner;
    }

    private void saveSellerProfile(Long accountId, String contactEmail) {
        SellerAccountEntity entity = new SellerAccountEntity();
        entity.setAccountId(accountId);
        entity.setOnboardingStatus(SellerOnboardingStatus.NOT_STARTED);
        entity.setContactEmail(contactEmail);
        sellerAccountRepository.save(entity);
    }
}

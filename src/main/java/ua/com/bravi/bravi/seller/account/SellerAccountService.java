package ua.com.bravi.bravi.seller.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.AccountView;
import ua.com.bravi.bravi.seller.account.api.SellerAccountRegistration;
import ua.com.bravi.bravi.seller.account.api.SellerAccountView;
import ua.com.bravi.bravi.seller.account.api.SellerAccountsApi;
import ua.com.bravi.bravi.seller.account.domain.SellerOnboardingStatus;
import ua.com.bravi.bravi.seller.account.exception.SellerAccountAlreadyExistsException;
import ua.com.bravi.bravi.seller.account.persistence.ISellerAccountRepository;
import ua.com.bravi.bravi.seller.account.persistence.entity.SellerAccountEntity;
import ua.com.bravi.bravi.seller.account.persistence.mapper.SellerAccountEntityMapper;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.exception.ForbiddenException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerAccountService implements SellerAccountsApi {

    private static final String SELLER = "SELLER";

    private final AccessApi accessApi;
    private final ISellerAccountRepository sellerAccountRepository;
    private final SellerAccountEntityMapper sellerAccountEntityMapper;
    private final InvocationContext invocationContext;

    @Override
    @Transactional
    public SellerAccountView onboardCurrentUser(SellerAccountRegistration registration) {
        Long userId = invocationContext.getUserId();
        if (userId == null) {
            throw new ForbiddenException("User is not resolved");
        }

        boolean alreadySeller = accessApi.findAccountsByCurrentUser().stream()
                .anyMatch(account -> SELLER.equals(account.type()));
        if (alreadySeller) {
            throw new SellerAccountAlreadyExistsException("User already has a seller account");
        }

        AccountView account = accessApi.provisionOwnerAccount(userId, SELLER);

        SellerAccountEntity entity = new SellerAccountEntity();
        entity.setAccountId(account.id());
        entity.setLegalName(registration.legalName());
        entity.setOnboardingStatus(SellerOnboardingStatus.ACTIVE);
        entity.setContactEmail(registration.contactEmail());
        entity.setPhone(registration.phone());
        SellerAccountEntity saved = sellerAccountRepository.save(entity);

        return sellerAccountEntityMapper.toView(saved, account.publicId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SellerAccountView> findByAccountId(Long accountId) {
        return sellerAccountRepository.findById(accountId)
                .map(entity -> sellerAccountEntityMapper.toView(
                        entity,
                        accessApi.findAccountById(accountId).map(AccountView::publicId).orElse(null)));
    }
}

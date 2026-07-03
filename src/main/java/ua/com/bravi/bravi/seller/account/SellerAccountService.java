package ua.com.bravi.bravi.seller.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.AccountView;
import ua.com.bravi.bravi.seller.account.api.SellerAccountView;
import ua.com.bravi.bravi.seller.account.api.SellerAccountsApi;
import ua.com.bravi.bravi.seller.account.domain.SellerOnboardingStatus;
import ua.com.bravi.bravi.seller.account.persistence.ISellerAccountRepository;
import ua.com.bravi.bravi.seller.account.persistence.entity.SellerAccountEntity;
import ua.com.bravi.bravi.seller.account.persistence.mapper.SellerAccountEntityMapper;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerAccountService implements SellerAccountsApi {

    private final AccessApi accessApi;
    private final ISellerAccountRepository sellerAccountRepository;
    private final SellerAccountEntityMapper sellerAccountEntityMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<SellerAccountView> findByAccountId(Long accountId) {
        return sellerAccountRepository.findById(accountId)
                .map(entity -> sellerAccountEntityMapper.toView(
                        entity,
                        accessApi.findAccountById(accountId).map(AccountView::publicId).orElse(null)));
    }

    @Override
    @Transactional
    public void updateOnboardingStatus(Long accountId, String onboardingStatus) {
        SellerAccountEntity entity = sellerAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Seller account not found"));
        entity.setOnboardingStatus(SellerOnboardingStatus.valueOf(onboardingStatus));
    }
}

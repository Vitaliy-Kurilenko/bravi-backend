package ua.com.bravi.bravi.seller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.AccountMembershipView;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.api.IdentityApi;
import ua.com.bravi.bravi.seller.account.api.SellerAccountView;
import ua.com.bravi.bravi.seller.account.api.SellerAccountsApi;
import ua.com.bravi.bravi.seller.controller.dto.out.AccountsResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.AccountsResponse.AccountItemResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.AccountsResponse.SellerResponse;
import ua.com.bravi.bravi.seller.controller.mapper.AccountDtoMapper;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.List;

/**
 * Assembles the {@code GET /accounts} read model from identity + access + seller.account.
 * Also performs the one-way {@code email_verified} sync (false → true) from the JWT.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String SELLER = "SELLER";

    private final IdentityApi identityApi;
    private final AccessApi accessApi;
    private final SellerAccountsApi sellerAccountsApi;
    private final InvocationContext invocationContext;
    private final AccountDtoMapper accountDtoMapper;

    @Transactional
    public AccountsResponse currentUserAccounts() {
        Long userId = invocationContext.getUserId();
        if (userId == null) {
            throw new NotFoundException("User context not found");
        }

        CurrentUserView user = identityApi.getById(userId);
        if (invocationContext.isTokenEmailVerified() && !user.emailVerified()) {
            user = identityApi.syncEmailVerified(userId, true);
        }

        List<AccountItemResponse> accounts = accessApi.findAccountMembershipsByCurrentUser().stream()
                .map(this::toAccountResponse)
                .toList();

        return new AccountsResponse(accountDtoMapper.toUserResponse(user), accounts);
    }

    private AccountItemResponse toAccountResponse(AccountMembershipView membership) {
        SellerResponse seller = null;
        if (SELLER.equals(membership.type())) {
            String onboardingStatus = sellerAccountsApi.findByAccountId(membership.accountId())
                    .map(SellerAccountView::onboardingStatus)
                    .orElse(null);
            seller = onboardingStatus == null ? null : new SellerResponse(onboardingStatus);
        }
        return new AccountItemResponse(
                membership.accountPublicId(),
                membership.type(),
                membership.roleCodes().isEmpty() ? null : membership.roleCodes().get(0),
                membership.status(),
                seller);
    }
}

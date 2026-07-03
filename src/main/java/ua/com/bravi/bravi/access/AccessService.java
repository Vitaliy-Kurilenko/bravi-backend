package ua.com.bravi.bravi.access;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.AccessContextView;
import ua.com.bravi.bravi.access.api.AccountView;
import ua.com.bravi.bravi.access.domain.AccountStatus;
import ua.com.bravi.bravi.access.domain.AccountType;
import ua.com.bravi.bravi.access.domain.MembershipStatus;
import ua.com.bravi.bravi.access.persistence.IAccountRepository;
import ua.com.bravi.bravi.access.persistence.IMembershipRepository;
import ua.com.bravi.bravi.access.persistence.entity.AccountEntity;
import ua.com.bravi.bravi.access.persistence.entity.MembershipEntity;
import ua.com.bravi.bravi.access.persistence.mapper.AccountEntityMapper;
import ua.com.bravi.bravi.shared.component.InvocationContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessService implements AccessApi {

    private final IAccountRepository accountRepository;
    private final IMembershipRepository membershipRepository;
    private final AccountEntityMapper accountEntityMapper;
    private final InvocationContext invocationContext;

    @Override
    @Transactional(readOnly = true)
    public Optional<AccessContextView> resolveCurrentContext() {
        Long userId = invocationContext.getUserId();
        if (userId == null) {
            return Optional.empty();
        }

        List<MembershipEntity> activeMemberships =
                membershipRepository.findByUserIdAndStatusOrderByIdAsc(userId, MembershipStatus.ACTIVE);
        if (activeMemberships.isEmpty()) {
            return Optional.empty();
        }

        // TODO(seller/supplier step): multi-account selection via request header.
        // For now the first active membership is the current account.
        Long accountId = activeMemberships.get(0).getAccountId();

        return accountRepository.findById(accountId)
                .map(account -> new AccessContextView(
                        account.getId(),
                        account.getPublicId(),
                        account.getType().name(),
                        membershipRepository.findRoleCodes(userId, accountId),
                        membershipRepository.findPermissionCodes(userId, accountId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountView> findAccountsByCurrentUser() {
        Long userId = invocationContext.getUserId();
        if (userId == null) {
            return List.of();
        }
        return membershipRepository.findByUserId(userId).stream()
                .map(MembershipEntity::getAccountId)
                .distinct()
                .map(accountRepository::findById)
                .flatMap(Optional::stream)
                .map(accountEntityMapper::toDomain)
                .map(accountEntityMapper::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountView> findAccountById(Long id) {
        return accountRepository.findById(id)
                .map(accountEntityMapper::toDomain)
                .map(accountEntityMapper::toView);
    }

    @Override
    @Transactional
    public AccountView provisionOwnerAccount(Long userId, String accountType) {
        AccountType type = AccountType.valueOf(accountType);

        AccountEntity account = new AccountEntity();
        account.setPublicId(UUID.randomUUID().toString());
        account.setType(type);
        account.setStatus(AccountStatus.ACTIVE);
        AccountEntity savedAccount = accountRepository.save(account);

        MembershipEntity membership = new MembershipEntity();
        membership.setPublicId(UUID.randomUUID().toString());
        membership.setUserId(userId);
        membership.setAccountId(savedAccount.getId());
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());
        MembershipEntity savedMembership = membershipRepository.save(membership);

        membershipRepository.assignSystemRole(savedMembership.getId(), type.name() + "_OWNER");

        return accountEntityMapper.toView(accountEntityMapper.toDomain(savedAccount));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean currentUserHasPermission(String permissionCode) {
        return resolveCurrentContext()
                .map(ctx -> ctx.permissionCodes().contains(permissionCode))
                .orElse(false);
    }
}

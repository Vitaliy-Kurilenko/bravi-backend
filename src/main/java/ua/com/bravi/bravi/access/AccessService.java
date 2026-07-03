package ua.com.bravi.bravi.access;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.AccessContextView;
import ua.com.bravi.bravi.access.api.AccountView;
import ua.com.bravi.bravi.access.api.OwnerAccountView;
import ua.com.bravi.bravi.access.domain.AccountStatus;
import ua.com.bravi.bravi.access.domain.AccountType;
import ua.com.bravi.bravi.access.domain.MembershipStatus;
import ua.com.bravi.bravi.access.persistence.IAccountRepository;
import ua.com.bravi.bravi.access.persistence.IMembershipRepository;
import ua.com.bravi.bravi.access.persistence.entity.AccountEntity;
import ua.com.bravi.bravi.access.persistence.entity.MembershipEntity;
import ua.com.bravi.bravi.access.persistence.mapper.AccountEntityMapper;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    @Transactional(readOnly = true)
    public Optional<OwnerAccountView> findOwnerAccount(Long userId, String accountType) {
        AccountType type = AccountType.valueOf(accountType);
        return membershipRepository.findByUserId(userId).stream()
                .flatMap(membership -> accountRepository.findById(membership.getAccountId()).stream()
                        .filter(account -> account.getType() == type)
                        .map(account -> new OwnerAccountView(
                                account.getId(),
                                account.getPublicId(),
                                account.getType().name(),
                                account.getStatus().name(),
                                membership.getId(),
                                membership.getPublicId())))
                .findFirst();
    }

    @Override
    @Transactional
    public OwnerAccountView provisionOwnerAccount(Long userId, String accountType) {
        AccountType type = AccountType.valueOf(accountType);

        AccountEntity account = new AccountEntity();
        account.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.ACCOUNT_PREFIX));
        account.setType(type);
        account.setStatus(AccountStatus.PENDING_ONBOARDING);
        AccountEntity savedAccount = accountRepository.save(account);

        MembershipEntity membership = new MembershipEntity();
        membership.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.MEMBERSHIP_PREFIX));
        membership.setUserId(userId);
        membership.setAccountId(savedAccount.getId());
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());
        MembershipEntity savedMembership = membershipRepository.save(membership);

        membershipRepository.assignSystemRole(savedMembership.getId(), type.name() + "_OWNER");

        return new OwnerAccountView(
                savedAccount.getId(),
                savedAccount.getPublicId(),
                savedAccount.getType().name(),
                savedAccount.getStatus().name(),
                savedMembership.getId(),
                savedMembership.getPublicId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean currentUserHasPermission(String permissionCode) {
        return resolveCurrentContext()
                .map(ctx -> ctx.permissionCodes().contains(permissionCode))
                .orElse(false);
    }
}

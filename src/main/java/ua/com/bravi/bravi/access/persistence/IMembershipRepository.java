package ua.com.bravi.bravi.access.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.com.bravi.bravi.access.domain.MembershipStatus;
import ua.com.bravi.bravi.access.persistence.entity.MembershipEntity;

import java.util.List;

public interface IMembershipRepository extends JpaRepository<MembershipEntity, Long> {

    List<MembershipEntity> findByUserId(Long userId);

    List<MembershipEntity> findByUserIdAndStatusOrderByIdAsc(Long userId, MembershipStatus status);

    @Query(value = """
            SELECT DISTINCT r.code
            FROM memberships m
                     JOIN membership_roles mr ON mr.membership_id = m.id
                     JOIN roles r ON r.id = mr.role_id
            WHERE m.user_id = :userId
              AND m.account_id = :accountId
              AND m.status = 'ACTIVE'
            """, nativeQuery = true)
    List<String> findRoleCodes(@Param("userId") Long userId, @Param("accountId") Long accountId);

    @Query(value = """
            SELECT DISTINCT p.code
            FROM memberships m
                     JOIN membership_roles mr ON mr.membership_id = m.id
                     JOIN role_permissions rp ON rp.role_id = mr.role_id
                     JOIN permissions p ON p.id = rp.permission_id
            WHERE m.user_id = :userId
              AND m.account_id = :accountId
              AND m.status = 'ACTIVE'
            """, nativeQuery = true)
    List<String> findPermissionCodes(@Param("userId") Long userId, @Param("accountId") Long accountId);
}

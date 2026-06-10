package ua.com.bravi.bravi.users.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.com.bravi.bravi.users.domain.UserStatus;
import ua.com.bravi.bravi.users.domain.UserType;
import ua.com.bravi.bravi.users.persistence.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface IUserEntityRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByExtId(UUID extId);

    @Query("""
            SELECT u.id AS userId,
                   u.extId AS userExtId,
                   u.type AS userType,
                   u.status AS userStatus,
                   u.firstName AS firstName,
                   u.lastName AS lastName,
                   u.email AS email
            FROM UserEntity u
            WHERE u.extId = :extId
            """)
    Optional<UserContextProjection> findContextByExtId(@Param("extId") UUID extId);

    interface UserContextProjection {
        Long getUserId();
        UUID getUserExtId();
        UserType getUserType();
        UserStatus getUserStatus();
        String getFirstName();
        String getLastName();
        String getEmail();
    }
}

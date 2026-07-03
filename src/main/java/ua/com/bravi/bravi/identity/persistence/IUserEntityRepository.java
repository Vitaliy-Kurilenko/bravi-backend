package ua.com.bravi.bravi.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.com.bravi.bravi.identity.domain.UserStatus;
import ua.com.bravi.bravi.identity.persistence.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface IUserEntityRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByExtId(UUID extId);

    Optional<UserEntity> findByEmail(String email);

    @Query("""
            SELECT u.id AS userId,
                   u.publicId AS userPublicId,
                   u.extId AS userExtId,
                   u.status AS userStatus,
                   u.emailVerified AS emailVerified,
                   u.firstName AS firstName,
                   u.lastName AS lastName,
                   u.email AS email
            FROM UserEntity u
            WHERE u.extId = :extId
            """)
    Optional<UserContextProjection> findContextByExtId(@Param("extId") UUID extId);

    interface UserContextProjection {
        Long getUserId();
        String getUserPublicId();
        UUID getUserExtId();
        UserStatus getUserStatus();
        boolean isEmailVerified();
        String getFirstName();
        String getLastName();
        String getEmail();
    }
}

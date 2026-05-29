package ua.com.bravi.bravi.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.persistance.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface IUserEntityRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByExtId(UUID extId);
}

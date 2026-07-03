package ua.com.bravi.bravi.access.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.access.persistence.entity.AccountEntity;

import java.util.Optional;

public interface IAccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByPublicId(String publicId);
}

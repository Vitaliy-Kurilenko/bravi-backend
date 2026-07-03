package ua.com.bravi.bravi.identity.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.identity.domain.UserStatus;
import ua.com.bravi.bravi.identity.persistence.entity.UserEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserEntityRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private IUserEntityRepository repository;

    private static UserEntity newUser(UUID extId) {
        UserEntity user = new UserEntity();
        user.setExtId(extId);
        user.setFirstName("John");
        user.setEmail("john@example.com");
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        return user;
    }

    @Test
    void savesAndLoadsUser() {
        UserEntity saved = repository.saveAndFlush(newUser(UUID.randomUUID()));

        UserEntity loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getFirstName()).isEqualTo("John");
        assertThat(loaded.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
    }

    @Test
    void prePersistSetsCreatedAt() {
        UserEntity saved = repository.saveAndFlush(newUser(UUID.randomUUID()));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();
    }

    @Test
    void enforcesUniqueExtId() {
        UUID extId = UUID.randomUUID();
        repository.saveAndFlush(newUser(extId));

        assertThatThrownBy(() -> repository.saveAndFlush(newUser(extId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsByExtId() {
        UUID extId = UUID.randomUUID();
        repository.saveAndFlush(newUser(extId));

        assertThat(repository.findByExtId(extId)).isPresent();
        assertThat(repository.findByExtId(UUID.randomUUID())).isEmpty();
    }
}

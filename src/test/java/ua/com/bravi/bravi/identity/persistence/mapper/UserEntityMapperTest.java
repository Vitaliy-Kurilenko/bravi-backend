package ua.com.bravi.bravi.identity.persistence.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.com.bravi.bravi.identity.domain.User;
import ua.com.bravi.bravi.identity.domain.UserStatus;
import ua.com.bravi.bravi.identity.persistence.entity.UserEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityMapperTest {

    private final UserEntityMapper mapper = Mappers.getMapper(UserEntityMapper.class);

    @Test
    void toDomainMapsAllFields() {
        UUID extId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setPublicId("usr_Jane");
        entity.setExtId(extId);
        entity.setFirstName("Jane");
        entity.setLastName("Roe");
        entity.setEmail("jane@example.com");
        entity.setEmailVerified(true);
        entity.setStatus(UserStatus.ACTIVE);

        User user = mapper.toDomain(entity);

        assertThat(user.publicId()).isEqualTo("usr_Jane");
        assertThat(user.extId()).isEqualTo(extId);
        assertThat(user.firstName()).isEqualTo("Jane");
        assertThat(user.lastName()).isEqualTo("Roe");
        assertThat(user.email()).isEqualTo("jane@example.com");
        assertThat(user.emailVerified()).isTrue();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void toEntityIgnoresIdAndTimestamps() {
        User user = new User(99L, "usr_John", UUID.randomUUID(),
                "John", "Doe", "john@example.com", false, UserStatus.ACTIVE);

        UserEntity entity = mapper.toEntity(user);

        assertThat(entity.getId()).isZero();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getPublicId()).isEqualTo("usr_John");
        assertThat(entity.getExtId()).isEqualTo(user.extId());
        assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}

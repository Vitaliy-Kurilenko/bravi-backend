package ua.com.bravi.bravi.users.persistence.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.com.bravi.bravi.users.domain.User;
import ua.com.bravi.bravi.users.domain.UserStatus;
import ua.com.bravi.bravi.users.domain.UserType;
import ua.com.bravi.bravi.users.persistence.entity.UserEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityMapperTest {

    private final UserEntityMapper mapper = Mappers.getMapper(UserEntityMapper.class);

    @Test
    void toDomainMapsAllFields() {
        UUID extId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setExtId(extId);
        entity.setType(UserType.BUYER);
        entity.setFirstName("Jane");
        entity.setLastName("Roe");
        entity.setEmail("jane@example.com");
        entity.setStatus(UserStatus.ACTIVE);

        User user = mapper.toDomain(entity);

        assertThat(user.extId()).isEqualTo(extId);
        assertThat(user.type()).isEqualTo(UserType.BUYER);
        assertThat(user.firstName()).isEqualTo("Jane");
        assertThat(user.lastName()).isEqualTo("Roe");
        assertThat(user.email()).isEqualTo("jane@example.com");
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void toEntityIgnoresIdAndTimestamps() {
        User user = new User(99L, UUID.randomUUID(), UserType.SELLER,
                "John", "Doe", "john@example.com", UserStatus.ACTIVE);

        UserEntity entity = mapper.toEntity(user);

        assertThat(entity.getId()).isZero();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getExtId()).isEqualTo(user.extId());
        assertThat(entity.getType()).isEqualTo(UserType.SELLER);
        assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}

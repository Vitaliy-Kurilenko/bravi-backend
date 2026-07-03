package ua.com.bravi.bravi.identity.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void provisionNewActivatesUserWithoutId() {
        UUID extId = UUID.randomUUID();

        User user = User.provisionNew(extId, "John", "Doe", "john@example.com", UserType.SELLER);

        assertThat(user.id()).isNull();
        assertThat(user.extId()).isEqualTo(extId);
        assertThat(user.firstName()).isEqualTo("John");
        assertThat(user.lastName()).isEqualTo("Doe");
        assertThat(user.email()).isEqualTo("john@example.com");
        assertThat(user.type()).isEqualTo(UserType.SELLER);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }
}

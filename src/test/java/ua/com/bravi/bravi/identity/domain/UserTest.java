package ua.com.bravi.bravi.identity.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void registerActivatesUserWithoutIdAndUnverifiedEmail() {
        UUID extId = UUID.randomUUID();

        User user = User.register("usr_John", extId, "John", "Doe", "john@example.com");

        assertThat(user.id()).isNull();
        assertThat(user.publicId()).isEqualTo("usr_John");
        assertThat(user.extId()).isEqualTo(extId);
        assertThat(user.firstName()).isEqualTo("John");
        assertThat(user.lastName()).isEqualTo("Doe");
        assertThat(user.email()).isEqualTo("john@example.com");
        assertThat(user.emailVerified()).isFalse();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }
}

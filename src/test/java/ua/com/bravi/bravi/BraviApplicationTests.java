package ua.com.bravi.bravi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class BraviApplicationTests extends AbstractPostgresIT {

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            JwtDecoder decoder = mock(JwtDecoder.class);
            when(decoder.decode(anyString())).thenReturn(mock(Jwt.class));
            return decoder;
        }
    }

    @Test
    void contextLoads() {
    }

}

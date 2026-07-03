package ua.com.bravi.bravi.shared.component;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import ua.com.bravi.bravi.shared.common.HttpConstants;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvocationContextFilterTest {

    private static final UUID SUBJECT = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final InvocationContext context = new InvocationContext();
    private final UserAgentParser userAgentParser = mock(UserAgentParser.class);
    private final InvocationContextFilter filter = new InvocationContextFilter(context, userAgentParser);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesContextFromJwtAndUserAgent() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(SUBJECT.toString())
                .claim("preferred_username", "john.doe")
                .claim("email", "john@example.com")
                .claim("given_name", "John")
                .claim("family_name", "Doe")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        var auth = new JwtAuthenticationToken(jwt, List.of(
                new SimpleGrantedAuthority("ROLE_admin"),
                new SimpleGrantedAuthority("seller")
        ));
        SecurityContextHolder.getContext().setAuthentication(auth);

        DeviceInfo device = DeviceInfo.unknown("ua");
        when(userAgentParser.parse("ua")).thenReturn(device);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/test");
        request.addHeader(HttpConstants.REQUEST_ID_HEADER, "corr-9");
        request.addHeader(HttpConstants.USER_AGENT_HEADER, "ua");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(context.getRequestId()).isEqualTo("corr-9");
        assertThat(context.getUserExtId()).isEqualTo(SUBJECT);
        assertThat(context.getUsername()).isEqualTo("john.doe");
        assertThat(context.getEmail()).isEqualTo("john@example.com");
        assertThat(context.getRoles()).containsExactlyInAnyOrder("admin", "seller");
        assertThat(context.getDevice()).isSameAs(device);
        assertThat(context.getFirstName()).isEqualTo("John");
        assertThat(context.getLastName()).isEqualTo("Doe");
        verify(chain).doFilter(request, response);
    }

    @Test
    void firstNameFallsBackToUsername() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(SUBJECT.toString())
                .claim("preferred_username", "john.doe")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
        when(userAgentParser.parse(null)).thenReturn(DeviceInfo.unknown(null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/test");
        request.addHeader(HttpConstants.REQUEST_ID_HEADER, "corr-2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(context.getFirstName()).isEqualTo("john.doe");
        assertThat(context.getLastName()).isNull();
    }

    @Test
    void leavesIdentityNullWhenAuthenticationIsNotJwt() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass", List.of())
        );
        when(userAgentParser.parse(null)).thenReturn(DeviceInfo.unknown(null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/test");
        request.addHeader(HttpConstants.REQUEST_ID_HEADER, "corr-x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(context.getRequestId()).isEqualTo("corr-x");
        assertThat(context.getUserExtId()).isNull();
        assertThat(context.getUsername()).isNull();
        assertThat(context.getRoles()).isNull();
        assertThat(context.getDevice()).isNotNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsExcludedPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs/swagger-config");
        request.setServletPath("/v3/api-docs/swagger-config");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }
}

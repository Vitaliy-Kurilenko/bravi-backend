package ua.com.bravi.bravi.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.component.ProblemDetailAccessDeniedHandler;
import ua.com.bravi.bravi.shared.component.ProblemDetailAuthenticationEntryPoint;
import ua.com.bravi.bravi.shared.config.props.SecurityProperties;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String BACKEND_SERVICE_CLAIM = "backend-service";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "";

    private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;
    private final SecurityProperties securityProperties;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String[] permitted = HttpConstants.EXCLUDED_PATHS.toArray(String[]::new);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitted).permitAll()
                        .requestMatchers(HttpConstants.INTERNAL_PATHS).hasAuthority(securityProperties.internalRole())
                        .anyRequest().hasAuthority(securityProperties.userRole())
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

    /**
     * Routes SpEL {@code hasPermission(resource, action)} in {@code @PreAuthorize} to the
     * DB-backed {@link PermissionEvaluator} (access module's AccessPermissionEvaluator).
     * Injected by interface so this cross-cutting config stays free of access-module imports.
     */
    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    private JwtAuthenticationConverter keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> backendServiceRoles(jwt).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList());
        return converter;
    }

    /**
     * All authorities come from the {@code backend-service} client roles: {@code auth_service}
     * for the Auth Service's service account, {@code bravi_user} (the "may call backend" pass) for
     * human users. Both token kinds address this backend, so both carry its client roles — Keycloak's
     * default {@code roles} scope surfaces them in {@code resource_access.backend-service.roles}.
     */
    private Collection<String> backendServiceRoles(Jwt jwt) {
        Object resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess instanceof Map<?, ?> resourceAccessMap
                && resourceAccessMap.get(BACKEND_SERVICE_CLAIM) instanceof Map<?, ?> backendServiceMap
                && backendServiceMap.get(ROLES_CLAIM) instanceof Collection<?> roles) {
            return roles.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}

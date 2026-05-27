package ua.com.bravi.bravi.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ua.com.bravi.bravi.util.HttpConstants;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class InvocationContextFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String EMAIL_CLAIM = "email";

    private final InvocationContext context;
    private final UserAgentParser userAgentParser;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return HttpConstants.EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        context.setRequestId(request.getHeader(HttpConstants.REQUEST_ID_HEADER));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            context.setUserExtId(UUID.fromString(jwt.getSubject()));
            context.setUsername(jwt.getClaimAsString(PREFERRED_USERNAME_CLAIM));
            context.setEmail(jwt.getClaimAsString(EMAIL_CLAIM));
            context.setRoles(extractRoles(jwtAuth));
        }

        context.setDevice(userAgentParser.parse(request.getHeader(HttpConstants.USER_AGENT_HEADER)));

        filterChain.doFilter(request, response);
    }

    private Set<String> extractRoles(JwtAuthenticationToken auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith(ROLE_PREFIX)
                        ? authority.substring(ROLE_PREFIX.length())
                        : authority)
                .collect(Collectors.toUnmodifiableSet());
    }
}

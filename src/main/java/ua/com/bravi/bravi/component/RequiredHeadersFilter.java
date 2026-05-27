package ua.com.bravi.bravi.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ua.com.bravi.bravi.util.HttpConstants;
import ua.com.bravi.bravi.exception.MissingRequiredHeaderException;

import java.io.IOException;

@RequiredArgsConstructor
public class RequiredHeadersFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;
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
        for (String headerName : HttpConstants.REQUIRED_HEADERS) {
            String value = request.getHeader(headerName);
            if (value == null || value.isBlank()) {
                handlerExceptionResolver.resolveException(
                        request,
                        response,
                        null,
                        new MissingRequiredHeaderException(headerName)
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

package ua.com.bravi.bravi.shared.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ua.com.bravi.bravi.shared.common.HttpConstants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Один рядок логу на завершений HTTP-запит.
 * Логер названий окремо від пакетів застосунку, щоб рівень керувався незалежно
 * (`logging.level.ua.com.bravi.bravi.access`).
 */
public class AccessLogFilter extends OncePerRequestFilter {

    public static final String LOGGER_NAME = "ua.com.bravi.bravi.access";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return HttpConstants.EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            String method = request.getMethod();
            String path = request.getRequestURI();
            int status = response.getStatus();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            // key-value пари стають окремими полями JSON у structured-профілі,
            // а сформоване повідомлення лишає рядок читабельним у текстовому
            log.atInfo()
                    .addKeyValue("method", method)
                    .addKeyValue("path", path)
                    .addKeyValue("status", status)
                    .addKeyValue("durationMs", durationMs)
                    .log("{} {} {} {}ms", method, path, status, durationMs);
        }
    }
}

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
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.common.LoggingConstants;
import ua.com.bravi.bravi.shared.util.LogSanitizer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Diagnostic logging of request and response bodies together with query parameters.
 * Stays off until the {@link LoggingConstants#PAYLOAD_LOGGER} logger is set to DEBUG.
 * Values of sensitive fields are masked by {@link LogSanitizer} even at DEBUG.
 */
public class PayloadLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingConstants.PAYLOAD_LOGGER);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!log.isDebugEnabled()) {
            return true;
        }
        String path = request.getServletPath();
        return HttpConstants.EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest =
                new ContentCachingRequestWrapper(request, LoggingConstants.MAX_CACHED_BODY_BYTES);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            logExchange(cachedRequest, cachedResponse);
            // Writes the buffered body out to the client.
            cachedResponse.copyBodyToResponse();
        }
    }

    private void logExchange(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        log.debug("--> {} {} query=[{}] body=[{}]",
                request.getMethod(),
                request.getRequestURI(),
                LogSanitizer.sanitize(request.getQueryString()),
                body(request.getContentAsByteArray(), request.getContentType(), request.getCharacterEncoding()));

        log.debug("<-- {} {} status={} body=[{}]",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                body(response.getContentAsByteArray(), response.getContentType(), response.getCharacterEncoding()));
    }

    private String body(byte[] content, String contentType, String encoding) {
        if (content.length == 0) {
            return "";
        }
        if (isSkipped(contentType)) {
            return "<" + contentType + ", " + content.length + " bytes>";
        }
        return LogSanitizer.sanitize(new String(content, charset(encoding)));
    }

    private boolean isSkipped(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase();
        return LoggingConstants.SKIPPED_CONTENT_TYPES.stream().anyMatch(lower::startsWith);
    }

    private Charset charset(String encoding) {
        try {
            return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        } catch (RuntimeException unsupported) {
            return StandardCharsets.UTF_8;
        }
    }
}

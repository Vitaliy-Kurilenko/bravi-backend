package ua.com.bravi.bravi.shared.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.common.MdcKeys;

import java.io.IOException;
import java.util.UUID;

/**
 * Fills the MDC with the context available from request headers.
 * Runs before Spring Security so that logs of 401 and 403 responses are correlated as well.
 */
public class MdcContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(MdcKeys.REQUEST_ID, requestId);
        response.setHeader(HttpConstants.REQUEST_ID_HEADER, requestId);

        putIfPresent(MdcKeys.ACCOUNT_ID, request.getHeader(HttpConstants.ACCOUNT_ID_HEADER));
        putIfPresent(MdcKeys.STORE_ID, request.getHeader(HttpConstants.STORE_ID_HEADER));

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.REQUEST_ID);
            MDC.remove(MdcKeys.ACCOUNT_ID);
            MDC.remove(MdcKeys.STORE_ID);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(HttpConstants.REQUEST_ID_HEADER);
        return StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
    }

    private void putIfPresent(String key, String value) {
        if (StringUtils.hasText(value)) {
            MDC.put(key, value);
        }
    }
}

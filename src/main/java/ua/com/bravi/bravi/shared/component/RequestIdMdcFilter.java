package ua.com.bravi.bravi.shared.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import ua.com.bravi.bravi.shared.common.HttpConstants;

import java.io.IOException;

public class RequestIdMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(HttpConstants.REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            MDC.put(HttpConstants.REQUEST_ID_MDC_KEY, requestId);
            response.setHeader(HttpConstants.REQUEST_ID_HEADER, requestId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(HttpConstants.REQUEST_ID_MDC_KEY);
        }
    }
}

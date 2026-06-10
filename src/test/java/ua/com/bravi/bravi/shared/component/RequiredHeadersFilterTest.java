package ua.com.bravi.bravi.shared.component;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ua.com.bravi.bravi.shared.exception.MissingRequiredHeaderException;
import ua.com.bravi.bravi.shared.common.HttpConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RequiredHeadersFilterTest {

    private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    private final RequiredHeadersFilter filter = new RequiredHeadersFilter(resolver);

    @Test
    void resolvesExceptionAndStopsChainWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/test");
        request.setServletPath("/users/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(MissingRequiredHeaderException.class)
                .extracting(ex -> ((MissingRequiredHeaderException) ex).getHeaderName())
                .isEqualTo(HttpConstants.REQUEST_ID_HEADER);
        verifyNoInteractions(chain);
    }

    @Test
    void proceedsWhenAllRequiredHeadersPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/test");
        request.setServletPath("/users/test");
        request.addHeader(HttpConstants.REQUEST_ID_HEADER, "corr-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(resolver, never()).resolveException(request, response, null, null);
    }

    @Test
    void skipsExcludedPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setServletPath("/actuator/health");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void filtersNonExcludedPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/test");
        request.setServletPath("/users/test");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }
}

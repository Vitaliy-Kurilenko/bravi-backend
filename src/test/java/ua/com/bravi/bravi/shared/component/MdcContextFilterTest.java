package ua.com.bravi.bravi.shared.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.common.MdcKeys;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcContextFilterTest {

    private final MdcContextFilter filter = new MdcContextFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void populatesMdcDuringChainAndEchoesHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpConstants.REQUEST_ID_HEADER, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(MdcKeys.REQUEST_ID));

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain.get()).isEqualTo("corr-123");
        assertThat(response.getHeader(HttpConstants.REQUEST_ID_HEADER)).isEqualTo("corr-123");
        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
    }

    @Test
    void populatesAccountAndStoreFromHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpConstants.REQUEST_ID_HEADER, "corr-scope");
        request.addHeader(HttpConstants.ACCOUNT_ID_HEADER, "acc_1");
        request.addHeader(HttpConstants.STORE_ID_HEADER, "str_2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, String> captured = new HashMap<>();
        FilterChain chain = (req, res) -> {
            captured.put(MdcKeys.ACCOUNT_ID, MDC.get(MdcKeys.ACCOUNT_ID));
            captured.put(MdcKeys.STORE_ID, MDC.get(MdcKeys.STORE_ID));
        };

        filter.doFilter(request, response, chain);

        assertThat(captured).containsEntry(MdcKeys.ACCOUNT_ID, "acc_1")
                .containsEntry(MdcKeys.STORE_ID, "str_2");
        assertThat(MDC.get(MdcKeys.ACCOUNT_ID)).isNull();
        assertThat(MDC.get(MdcKeys.STORE_ID)).isNull();
    }

    @Test
    void clearsMdcEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpConstants.REQUEST_ID_HEADER, "corr-err");
        request.addHeader(HttpConstants.ACCOUNT_ID_HEADER, "acc_err");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception ignored) {
        }

        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
        assertThat(MDC.get(MdcKeys.ACCOUNT_ID)).isNull();
    }

    @Test
    void generatesRequestIdWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(MdcKeys.REQUEST_ID));

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain.get()).isNotBlank();
        assertThat(response.getHeader(HttpConstants.REQUEST_ID_HEADER)).isEqualTo(mdcDuringChain.get());
        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
    }
}

package ua.com.bravi.bravi.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.component.InvocationContextFilter;
import ua.com.bravi.bravi.shared.component.RequestIdMdcFilter;
import ua.com.bravi.bravi.shared.component.RequiredHeadersFilter;
import ua.com.bravi.bravi.shared.component.UserAgentParser;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private static final int REQUIRED_HEADERS_ORDER = -200;
    private static final int REQUEST_ID_MDC_ORDER = -190;
    private static final int INVOCATION_CONTEXT_ORDER = 0;

    @Bean
    FilterRegistrationBean<RequiredHeadersFilter> requiredHeadersFilterRegistration(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        FilterRegistrationBean<RequiredHeadersFilter> registration =
                new FilterRegistrationBean<>(new RequiredHeadersFilter(handlerExceptionResolver));
        registration.setOrder(REQUIRED_HEADERS_ORDER);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RequestIdMdcFilter> requestIdMdcFilterRegistration() {
        FilterRegistrationBean<RequestIdMdcFilter> registration =
                new FilterRegistrationBean<>(new RequestIdMdcFilter());
        registration.setOrder(REQUEST_ID_MDC_ORDER);
        return registration;
    }

    @Bean
    FilterRegistrationBean<InvocationContextFilter> invocationContextFilterRegistration(
            InvocationContext invocationContext,
            UserAgentParser userAgentParser) {
        FilterRegistrationBean<InvocationContextFilter> registration =
                new FilterRegistrationBean<>(new InvocationContextFilter(invocationContext, userAgentParser));
        registration.setOrder(INVOCATION_CONTEXT_ORDER);
        return registration;
    }
}

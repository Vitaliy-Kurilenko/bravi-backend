package ua.com.bravi.bravi.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ua.com.bravi.bravi.shared.component.AccessLogFilter;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.component.InvocationContextFilter;
import ua.com.bravi.bravi.shared.component.MdcContextFilter;
import ua.com.bravi.bravi.shared.component.PayloadLoggingFilter;
import ua.com.bravi.bravi.shared.component.RequiredHeadersFilter;
import ua.com.bravi.bravi.shared.component.ServiceCallLoggingAspect;
import ua.com.bravi.bravi.shared.component.UserAgentParser;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private static final int REQUIRED_HEADERS_ORDER = -200;
    private static final int MDC_CONTEXT_ORDER = -190;
    private static final int ACCESS_LOG_ORDER = -180;
    private static final int PAYLOAD_LOG_ORDER = -170;
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
    FilterRegistrationBean<MdcContextFilter> mdcContextFilterRegistration() {
        FilterRegistrationBean<MdcContextFilter> registration =
                new FilterRegistrationBean<>(new MdcContextFilter());
        registration.setOrder(MDC_CONTEXT_ORDER);
        return registration;
    }

    @Bean
    FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration() {
        FilterRegistrationBean<AccessLogFilter> registration =
                new FilterRegistrationBean<>(new AccessLogFilter());
        registration.setOrder(ACCESS_LOG_ORDER);
        return registration;
    }

    @Bean
    FilterRegistrationBean<PayloadLoggingFilter> payloadLoggingFilterRegistration() {
        FilterRegistrationBean<PayloadLoggingFilter> registration =
                new FilterRegistrationBean<>(new PayloadLoggingFilter());
        registration.setOrder(PAYLOAD_LOG_ORDER);
        return registration;
    }

    @Bean
    ServiceCallLoggingAspect serviceCallLoggingAspect() {
        return new ServiceCallLoggingAspect();
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

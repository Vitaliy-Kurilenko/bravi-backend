package ua.com.bravi.bravi.access.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ua.com.bravi.bravi.access.component.AccountContextInterceptor;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.common.InterceptorOrder;

/**
 * Registers {@link AccountContextInterceptor} for the seller surface, ahead of the store-context
 * interceptor so the resolved account is available when the store is validated against it.
 */
@Configuration
@RequiredArgsConstructor
public class AccessWebConfig implements WebMvcConfigurer {

    private final AccountContextInterceptor accountContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accountContextInterceptor)
                .order(InterceptorOrder.RESOLVE_ACCOUNT_CONTEXT)
                .addPathPatterns("/sellers/**")
                .excludePathPatterns(HttpConstants.EXCLUDED_PATHS);
    }
}

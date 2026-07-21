package ua.com.bravi.bravi.seller.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ua.com.bravi.bravi.seller.component.SellerContextInterceptor;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.common.InterceptorOrder;

/**
 * Registers {@link SellerContextInterceptor} for the account/store-scoped seller surface,
 * ahead of {@code STORE_REQUIRED} so the resolved store is available to {@code @RequireStore}.
 */
@Configuration
@RequiredArgsConstructor
public class SellerWebConfig implements WebMvcConfigurer {

    private final SellerContextInterceptor sellerContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sellerContextInterceptor)
                .order(InterceptorOrder.RESOLVE_STORE_CONTEXT)
                .addPathPatterns("/stores/**", "/accounts/*/seller/**")
                .excludePathPatterns(HttpConstants.EXCLUDED_PATHS);
    }
}

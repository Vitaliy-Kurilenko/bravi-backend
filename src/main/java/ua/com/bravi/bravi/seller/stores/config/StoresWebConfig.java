package ua.com.bravi.bravi.seller.stores.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.common.InterceptorOrder;
import ua.com.bravi.bravi.seller.stores.component.StoreContextInterceptor;
import ua.com.bravi.bravi.seller.stores.component.StoreRequiredInterceptor;

@Configuration
@RequiredArgsConstructor
public class StoresWebConfig implements WebMvcConfigurer {

    private final StoreContextInterceptor storeContextInterceptor;
    private final StoreRequiredInterceptor storeRequiredInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(storeContextInterceptor)
                .order(InterceptorOrder.RESOLVE_STORE_CONTEXT)
                .addPathPatterns("/sellers/**")
                .excludePathPatterns(HttpConstants.EXCLUDED_PATHS);
        registry.addInterceptor(storeRequiredInterceptor)
                .order(InterceptorOrder.STORE_REQUIRED)
                .excludePathPatterns(HttpConstants.EXCLUDED_PATHS);
    }
}

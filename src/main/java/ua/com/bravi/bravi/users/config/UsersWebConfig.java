package ua.com.bravi.bravi.users.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ua.com.bravi.bravi.shared.common.HttpConstants;
import ua.com.bravi.bravi.shared.common.InterceptorOrder;
import ua.com.bravi.bravi.users.component.CurrentUserInterceptor;

@Configuration
@RequiredArgsConstructor
public class UsersWebConfig implements WebMvcConfigurer {

    private final CurrentUserInterceptor currentUserInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(currentUserInterceptor)
                .order(InterceptorOrder.CURRENT_USER)
                .excludePathPatterns(HttpConstants.EXCLUDED_PATHS);
    }
}

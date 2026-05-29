package ua.com.bravi.bravi.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ua.com.bravi.bravi.service.UserService;

@Component
@RequiredArgsConstructor
public class CurrentUserInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        userService.resolveCurrentUser();
        return true;
    }
}

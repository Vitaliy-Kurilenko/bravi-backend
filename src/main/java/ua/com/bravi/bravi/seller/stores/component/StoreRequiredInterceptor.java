package ua.com.bravi.bravi.seller.stores.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import ua.com.bravi.bravi.shared.component.PermitNoStore;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;

@Component
@RequiredArgsConstructor
public class StoreRequiredInterceptor implements HandlerInterceptor {

    private final CurrentStoreHolder currentStoreHolder;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!requiresStore(handlerMethod)) {
            return true;
        }
        if (currentStoreHolder.get() == null) {
            throw new NotFoundException("Store not found");
        }
        return true;
    }

    private boolean requiresStore(HandlerMethod handlerMethod) {
        if (handlerMethod.hasMethodAnnotation(PermitNoStore.class)) {
            return false;
        }
        if (handlerMethod.hasMethodAnnotation(RequireStore.class)) {
            return true;
        }
        return handlerMethod.getBeanType().isAnnotationPresent(RequireStore.class);
    }
}

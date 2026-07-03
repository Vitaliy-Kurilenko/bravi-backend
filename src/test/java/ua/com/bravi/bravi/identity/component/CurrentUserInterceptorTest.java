package ua.com.bravi.bravi.identity.component;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ua.com.bravi.bravi.identity.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CurrentUserInterceptorTest {

    private final UserService userService = mock(UserService.class);
    private final CurrentUserInterceptor interceptor = new CurrentUserInterceptor(userService);

    @Test
    void preHandleDelegatesToUserServiceAndProceeds() {
        boolean proceed = interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        verify(userService).resolveCurrentUser();
        assertThat(proceed).isTrue();
    }
}

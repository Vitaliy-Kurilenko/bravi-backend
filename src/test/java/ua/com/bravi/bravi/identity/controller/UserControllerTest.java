package ua.com.bravi.bravi.identity.controller;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.identity.UserService;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.controller.dto.out.UserResponse;
import ua.com.bravi.bravi.identity.controller.mapper.UserDtoMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserDtoMapper userDtoMapper = mock(UserDtoMapper.class);
    private final UserController controller = new UserController(userService, userDtoMapper);

    @Test
    void getUserContextMapsServiceResultToResponse() {
        CurrentUserView view = new CurrentUserView(5L, UUID.randomUUID(), "SELLER", "ACTIVE",
                "John", "Doe", "john@example.com");
        UserResponse response = new UserResponse("SELLER", "John", "Doe", "john@example.com", "ACTIVE");
        when(userService.getCurrentUserContext()).thenReturn(view);
        when(userDtoMapper.toUserResponse(view)).thenReturn(response);

        UserResponse result = controller.getUserContext();

        assertThat(result).isSameAs(response);
        verify(userService).getCurrentUserContext();
    }
}

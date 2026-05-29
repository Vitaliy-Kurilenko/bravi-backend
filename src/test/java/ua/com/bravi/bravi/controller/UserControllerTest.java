package ua.com.bravi.bravi.controller;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.controller.dto.out.UserResponse;
import ua.com.bravi.bravi.controller.mapper.UserDtoMapper;
import ua.com.bravi.bravi.domain.user.User;
import ua.com.bravi.bravi.domain.user.UserStatus;
import ua.com.bravi.bravi.domain.user.UserType;
import ua.com.bravi.bravi.service.UserService;

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
        User user = new User(5L, UUID.randomUUID(), UserType.SELLER,
                "John", "Doe", "john@example.com", UserStatus.ACTIVE);
        UserResponse response = new UserResponse("SELLER", "John", "Doe", "john@example.com", "ACTIVE");
        when(userService.getUserContext()).thenReturn(user);
        when(userDtoMapper.toUserResponse(user)).thenReturn(response);

        UserResponse result = controller.getUserContext();

        assertThat(result).isSameAs(response);
        verify(userService).getUserContext();
    }
}

package ua.com.bravi.bravi.users.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.users.UserService;
import ua.com.bravi.bravi.users.controller.dto.out.UserResponse;
import ua.com.bravi.bravi.users.controller.mapper.UserDtoMapper;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "UserController")
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @Operation(summary = "User context", description = "Returns the user's context")
    @GetMapping("/context")
    public UserResponse getUserContext() {
        return userDtoMapper.toUserResponse(userService.getCurrentUserContext());
    }

}

package ua.com.bravi.bravi.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.controller.dto.out.UserResponse;
import ua.com.bravi.bravi.domain.user.User;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    UserResponse toUserResponse(User user);

}

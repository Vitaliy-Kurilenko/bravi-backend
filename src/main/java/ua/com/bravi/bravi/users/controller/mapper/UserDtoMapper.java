package ua.com.bravi.bravi.users.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.users.api.CurrentUserView;
import ua.com.bravi.bravi.users.controller.dto.out.UserResponse;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    UserResponse toUserResponse(CurrentUserView user);

}

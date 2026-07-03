package ua.com.bravi.bravi.identity.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.controller.dto.out.UserResponse;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    UserResponse toUserResponse(CurrentUserView user);

}

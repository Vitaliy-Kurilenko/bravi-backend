package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.seller.controller.dto.out.AccountsResponse.UserResponse;

@Mapper(componentModel = "spring")
public interface AccountDtoMapper {

    @Mapping(target = "userId", source = "publicId")
    UserResponse toUserResponse(CurrentUserView view);
}

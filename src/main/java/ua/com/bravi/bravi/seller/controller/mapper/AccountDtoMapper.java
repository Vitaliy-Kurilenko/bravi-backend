package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.seller.controller.dto.out.AccountsResponse.AccountUserResponse;

@Mapper(componentModel = "spring")
public interface AccountDtoMapper {

    @Mapping(target = "userId", source = "publicId")
    AccountUserResponse toUserResponse(CurrentUserView view);
}

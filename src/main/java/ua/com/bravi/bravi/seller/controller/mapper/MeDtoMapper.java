package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.seller.controller.dto.out.MeResponse.MeUserResponse;

@Mapper(componentModel = "spring")
public interface MeDtoMapper {

    @Mapping(target = "userId", source = "publicId")
    MeUserResponse toUserResponse(CurrentUserView view);
}

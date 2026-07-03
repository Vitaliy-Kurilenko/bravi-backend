package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.seller.account.api.SellerRegistrationCommand;
import ua.com.bravi.bravi.seller.account.api.SellerRegistrationView;
import ua.com.bravi.bravi.seller.controller.dto.in.SellerRegistrationRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.SellerRegistrationResponse;

@Mapper(componentModel = "spring")
public interface SellerRegistrationDtoMapper {

    SellerRegistrationCommand toCommand(SellerRegistrationRequest request);

    SellerRegistrationResponse toResponse(SellerRegistrationView view);
}

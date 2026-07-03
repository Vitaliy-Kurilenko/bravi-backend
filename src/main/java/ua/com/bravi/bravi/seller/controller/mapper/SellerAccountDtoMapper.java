package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.seller.account.api.SellerAccountRegistration;
import ua.com.bravi.bravi.seller.account.api.SellerAccountView;
import ua.com.bravi.bravi.seller.controller.dto.in.SellerAccountCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.SellerAccountResponse;

@Mapper(componentModel = "spring")
public interface SellerAccountDtoMapper {

    SellerAccountRegistration toRegistration(SellerAccountCreateRequest request);

    SellerAccountResponse toResponse(SellerAccountView view);
}

package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.seller.controller.dto.out.ConfigFieldResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.PaymentMethodDefinitionResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StorePaymentMethodResponse;
import ua.com.bravi.bravi.stores.payments.api.ConfigFieldView;
import ua.com.bravi.bravi.stores.payments.api.PaymentMethodDefinitionView;
import ua.com.bravi.bravi.stores.payments.api.StorePaymentMethodView;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentDtoMapper {

    StorePaymentMethodResponse toResponse(StorePaymentMethodView view);

    List<StorePaymentMethodResponse> toResponses(List<StorePaymentMethodView> views);

    PaymentMethodDefinitionResponse toDefinitionResponse(PaymentMethodDefinitionView view);

    List<PaymentMethodDefinitionResponse> toDefinitionResponses(List<PaymentMethodDefinitionView> views);

    ConfigFieldResponse toConfigFieldResponse(ConfigFieldView view);
}

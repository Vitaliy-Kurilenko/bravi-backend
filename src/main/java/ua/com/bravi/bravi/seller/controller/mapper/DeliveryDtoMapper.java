package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.seller.controller.dto.out.ConfigFieldResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.DeliveryMethodDefinitionResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreDeliveryMethodResponse;
import ua.com.bravi.bravi.stores.delivery.api.ConfigFieldView;
import ua.com.bravi.bravi.stores.delivery.api.DeliveryMethodDefinitionView;
import ua.com.bravi.bravi.stores.delivery.api.StoreDeliveryMethodView;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeliveryDtoMapper {

    StoreDeliveryMethodResponse toResponse(StoreDeliveryMethodView view);

    List<StoreDeliveryMethodResponse> toResponses(List<StoreDeliveryMethodView> views);

    DeliveryMethodDefinitionResponse toDefinitionResponse(DeliveryMethodDefinitionView view);

    List<DeliveryMethodDefinitionResponse> toDefinitionResponses(List<DeliveryMethodDefinitionView> views);

    ConfigFieldResponse toConfigFieldResponse(ConfigFieldView view);
}

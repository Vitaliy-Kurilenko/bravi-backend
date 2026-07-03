package ua.com.bravi.bravi.seller.stores.delivery.persistence.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.seller.stores.delivery.api.StoreDeliveryMethodView;
import ua.com.bravi.bravi.seller.stores.delivery.persistence.entity.StoreDeliveryMethodEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreDeliveryMethodEntityMapper {

    StoreDeliveryMethodView toView(StoreDeliveryMethodEntity entity);

    List<StoreDeliveryMethodView> toViews(List<StoreDeliveryMethodEntity> entities);
}

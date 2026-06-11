package ua.com.bravi.bravi.stores.payments.persistence.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.stores.payments.api.StorePaymentMethodView;
import ua.com.bravi.bravi.stores.payments.persistence.entity.StorePaymentMethodEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StorePaymentMethodEntityMapper {

    StorePaymentMethodView toView(StorePaymentMethodEntity entity);

    List<StorePaymentMethodView> toViews(List<StorePaymentMethodEntity> entities);
}

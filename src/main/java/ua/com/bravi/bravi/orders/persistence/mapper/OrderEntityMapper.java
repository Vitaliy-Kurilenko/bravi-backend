package ua.com.bravi.bravi.orders.persistence.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.com.bravi.bravi.orders.api.OrderItemView;
import ua.com.bravi.bravi.orders.api.OrderShipmentView;
import ua.com.bravi.bravi.orders.api.OrderStatusView;
import ua.com.bravi.bravi.orders.api.OrderView;
import ua.com.bravi.bravi.orders.domain.Order;
import ua.com.bravi.bravi.orders.domain.OrderItem;
import ua.com.bravi.bravi.orders.persistence.entity.OrderEntity;
import ua.com.bravi.bravi.orders.persistence.entity.OrderItemEntity;
import ua.com.bravi.bravi.orders.persistence.entity.OrderShipmentEntity;
import ua.com.bravi.bravi.orders.persistence.entity.OrderStatusEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderEntityMapper {

    OrderView toView(OrderEntity entity);

    OrderItemView toItemView(OrderItemEntity entity);

    OrderItem toItemDomain(OrderItemEntity entity);

    List<OrderItem> toItemDomains(List<OrderItemEntity> entities);

    OrderShipmentView toShipmentView(OrderShipmentEntity entity);

    OrderStatusView toStatusView(OrderStatusEntity entity);

    List<OrderStatusView> toStatusViews(List<OrderStatusEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "statusId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discountTotal", ignore = true)
    @Mapping(target = "shippingTotal", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "shipment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toEntity(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderItemEntity toItemEntity(OrderItem item);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "buyerId", ignore = true)
    @Mapping(target = "statusId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discountTotal", ignore = true)
    @Mapping(target = "shippingTotal", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "shipment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget OrderEntity entity, Order patch);
}

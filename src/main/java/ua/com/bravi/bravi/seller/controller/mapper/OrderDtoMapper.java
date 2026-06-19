package ua.com.bravi.bravi.seller.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.com.bravi.bravi.orders.api.OrderPage;
import ua.com.bravi.bravi.orders.api.OrderStatusView;
import ua.com.bravi.bravi.orders.api.OrderView;
import ua.com.bravi.bravi.orders.domain.Order;
import ua.com.bravi.bravi.orders.domain.OrderItem;
import ua.com.bravi.bravi.orders.domain.OrderItemEdit;
import ua.com.bravi.bravi.seller.controller.dto.in.OrderCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OrderItemEditRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OrderItemRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OrderUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.OrderPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.OrderResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.OrderStatusResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDtoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "statusId", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "shipment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toDomain(OrderCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "partnerPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderItem toItem(OrderItemRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "storeId", ignore = true)
    @Mapping(target = "buyerId", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discountTotal", ignore = true)
    @Mapping(target = "shippingTotal", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "shipment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toDomain(OrderUpdateRequest request);

    OrderItemEdit toItemEdit(OrderItemRequest request);

    OrderItemEdit toItemEdit(OrderItemEditRequest request);

    OrderResponse toResponse(OrderView view);

    OrderStatusResponse toStatusResponse(OrderStatusView view);

    List<OrderStatusResponse> toStatusResponses(List<OrderStatusView> views);

    OrderPageResponse toPageResponse(OrderPage page);
}

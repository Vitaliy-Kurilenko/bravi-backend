package ua.com.bravi.bravi.seller.orders.api;

import ua.com.bravi.bravi.seller.orders.domain.DeliveryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderView(
        Long id,
        Long storeId,
        Long buyerId,
        OrderStatusView status,
        String recipientFirstName,
        String recipientLastName,
        String recipientPhone,
        String recipientEmail,
        String deliveryMethodCode,
        DeliveryType deliveryType,
        String deliveryCountry,
        String deliveryRegion,
        String deliveryCity,
        String deliveryAddress,
        String deliveryExtra,
        String deliveryWarehouseNo,
        String paymentMethodCode,
        BigDecimal prepayment,
        String currency,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal shippingTotal,
        BigDecimal total,
        String comment,
        String internalComment,
        List<OrderItemView> items,
        OrderShipmentView shipment,
        Instant createdAt,
        Instant updatedAt
) {
}

package ua.com.bravi.bravi.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(
        Long id,
        Long storeId,
        Long buyerId,
        Long statusId,
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
        List<OrderItem> items,
        OrderShipment shipment,
        Instant createdAt,
        Instant updatedAt
) {
}

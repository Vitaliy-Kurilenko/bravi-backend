package ua.com.bravi.bravi.seller.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import ua.com.bravi.bravi.seller.orders.domain.DeliveryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("buyer_id")
        Long buyerId,
        OrderStatusResponse status,
        @JsonProperty("recipient_first_name")
        String recipientFirstName,
        @JsonProperty("recipient_last_name")
        String recipientLastName,
        @JsonProperty("recipient_phone")
        String recipientPhone,
        @JsonProperty("recipient_email")
        String recipientEmail,
        @JsonProperty("delivery_method_code")
        String deliveryMethodCode,
        @JsonProperty("delivery_type")
        DeliveryType deliveryType,
        @JsonProperty("delivery_country")
        String deliveryCountry,
        @JsonProperty("delivery_region")
        String deliveryRegion,
        @JsonProperty("delivery_city")
        String deliveryCity,
        @JsonProperty("delivery_address")
        String deliveryAddress,
        @JsonProperty("delivery_extra")
        String deliveryExtra,
        @JsonProperty("delivery_warehouse_no")
        String deliveryWarehouseNo,
        @JsonProperty("payment_method_code")
        String paymentMethodCode,
        BigDecimal prepayment,
        String currency,
        BigDecimal subtotal,
        @JsonProperty("discount_total")
        BigDecimal discountTotal,
        @JsonProperty("shipping_total")
        BigDecimal shippingTotal,
        BigDecimal total,
        String comment,
        @JsonProperty("internal_comment")
        String internalComment,
        List<OrderItemResponse> items,
        OrderShipmentResponse shipment,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}

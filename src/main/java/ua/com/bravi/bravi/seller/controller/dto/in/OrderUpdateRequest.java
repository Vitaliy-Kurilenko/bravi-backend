package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import ua.com.bravi.bravi.seller.orders.domain.DeliveryType;

import java.math.BigDecimal;

public record OrderUpdateRequest(
        @JsonProperty("status_id")
        Long statusId,
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
        @PositiveOrZero
        BigDecimal prepayment,
        String currency,
        String comment,
        @JsonProperty("internal_comment")
        String internalComment
) {
}

package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ua.com.bravi.bravi.orders.domain.DeliveryType;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreateRequest(
        @JsonProperty("buyer_id")
        @NotNull
        Long buyerId,
        @JsonProperty("recipient_first_name")
        @NotBlank
        String recipientFirstName,
        @JsonProperty("recipient_last_name")
        @NotBlank
        String recipientLastName,
        @JsonProperty("recipient_phone")
        String recipientPhone,
        @JsonProperty("recipient_email")
        String recipientEmail,
        @JsonProperty("delivery_method_code")
        @NotBlank
        String deliveryMethodCode,
        @JsonProperty("delivery_type")
        @NotNull
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
        @NotBlank
        String paymentMethodCode,
        @PositiveOrZero
        BigDecimal prepayment,
        @NotBlank
        String currency,
        @JsonProperty("discount_total")
        @PositiveOrZero
        BigDecimal discountTotal,
        @JsonProperty("shipping_total")
        @PositiveOrZero
        BigDecimal shippingTotal,
        String comment,
        @JsonProperty("internal_comment")
        String internalComment,
        @Valid
        @NotEmpty
        List<OrderItemRequest> items
) {
}

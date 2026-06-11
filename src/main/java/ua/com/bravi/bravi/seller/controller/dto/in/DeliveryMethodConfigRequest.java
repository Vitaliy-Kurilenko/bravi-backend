package ua.com.bravi.bravi.seller.controller.dto.in;

import java.util.Map;

public record DeliveryMethodConfigRequest(
        Map<String, String> config
) {
}

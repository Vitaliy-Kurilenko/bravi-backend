package ua.com.bravi.bravi.seller.controller.dto.in;

import java.util.Map;

public record PaymentMethodConfigRequest(
        Map<String, String> config
) {
}

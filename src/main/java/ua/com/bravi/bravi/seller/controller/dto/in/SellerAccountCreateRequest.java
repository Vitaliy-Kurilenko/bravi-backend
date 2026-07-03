package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerAccountCreateRequest(
        @NotBlank
        @Size(max = 255)
        String legalName,

        @Email
        @Size(max = 255)
        String contactEmail,

        @Size(max = 64)
        String phone
) {
}

package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Seller-registration payload sent by the Auth Service after creating the Keycloak user. */
public record SellerRegistrationRequest(
        @NotNull
        UUID keycloakUserId,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 255)
        String firstName,

        @Size(max = 255)
        String lastName,

        String idempotencyKey
) {
}

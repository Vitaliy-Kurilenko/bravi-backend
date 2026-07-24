package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Body to request a presigned upload URL for a product image. */
public record ProductImageUploadUrlRequest(
        @NotBlank
        @JsonProperty("content_type")
        String contentType,
        @Positive
        @JsonProperty("file_size")
        long fileSize,
        String filename
) {
}

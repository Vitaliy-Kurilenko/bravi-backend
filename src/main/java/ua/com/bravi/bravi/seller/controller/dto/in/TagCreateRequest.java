package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ua.com.bravi.bravi.seller.tags.domain.TagColor;
import ua.com.bravi.bravi.seller.tags.domain.TagName;
import ua.com.bravi.bravi.seller.tags.domain.TagStatus;

/** {@code color} is optional: a tag submitted without one is given a colour from the palette. */
public record TagCreateRequest(
        @NotBlank
        @Size(max = TagName.MAX_LENGTH)
        String name,
        @Size(max = TagColor.LENGTH)
        String color,
        TagStatus status
) {
}

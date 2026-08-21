package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.Size;
import ua.com.bravi.bravi.seller.tags.domain.TagColor;
import ua.com.bravi.bravi.seller.tags.domain.TagName;
import ua.com.bravi.bravi.seller.tags.domain.TagStatus;

/** Every field is optional; an absent one is left as it is. */
public record TagUpdateRequest(
        @Size(max = TagName.MAX_LENGTH)
        String name,
        @Size(max = TagColor.LENGTH)
        String color,
        TagStatus status
) {
}

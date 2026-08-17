package ua.com.bravi.bravi.seller.catalog.attributes.api;

public record AttributeOptionView(
        String publicId,
        String code,
        String name,
        Integer sortOrder
) {
}

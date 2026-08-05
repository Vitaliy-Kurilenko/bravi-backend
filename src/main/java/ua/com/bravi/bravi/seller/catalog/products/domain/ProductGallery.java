package ua.com.bravi.bravi.seller.catalog.products.domain;

import ua.com.bravi.bravi.seller.catalog.products.exception.InvalidProductRequestException;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered gallery of a product's images: positions form a gap-free zero-based sequence and the
 * image at position 0 is the product's main one. Pure functions over the image ids of one product.
 */
public final class ProductGallery {

    public static final int PRIMARY_POSITION = 0;

    private static final String FIELD_SORT_ORDER = "sort_order";

    private final List<Long> imageIds;

    private ProductGallery(List<Long> imageIds) {
        this.imageIds = imageIds;
    }

    /** Builds a gallery from image ids already read in stored-position order. */
    public static ProductGallery of(List<Long> orderedImageIds) {
        return new ProductGallery(List.copyOf(orderedImageIds));
    }

    /** Tells whether an image at this position is the one shown as the product's main image. */
    public static boolean isPrimary(Integer position) {
        return position != null && position == PRIMARY_POSITION;
    }

    public int size() {
        return imageIds.size();
    }

    /** Position a newly attached image takes. */
    public int nextPosition() {
        return imageIds.size();
    }

    /** Ids in the order they take after moving {@code imageId} to {@code target}. */
    public List<Long> move(Long imageId, int target) {
        if (target < 0 || target >= imageIds.size()) {
            throw new InvalidProductRequestException(FIELD_SORT_ORDER,
                    "Position must be between 0 and " + (imageIds.size() - 1));
        }
        List<Long> moved = new ArrayList<>(imageIds);
        moved.remove(imageId);
        moved.add(target, imageId);
        return List.copyOf(moved);
    }

    /** Ids in the order they take after {@code imageId} leaves the gallery. */
    public List<Long> without(Long imageId) {
        return imageIds.stream()
                .filter(id -> !id.equals(imageId))
                .toList();
    }
}

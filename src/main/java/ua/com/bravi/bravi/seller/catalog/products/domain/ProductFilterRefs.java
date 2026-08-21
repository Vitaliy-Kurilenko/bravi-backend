package ua.com.bravi.bravi.seller.catalog.products.domain;

import java.util.List;

/**
 * Search filters already translated from public ids into internal ones. They travel together
 * because three bare lists of the same type, passed positionally, are a swap waiting to happen.
 * A null list means the filter is off; an empty one is treated the same way.
 */
public record ProductFilterRefs(
        List<Long> categoryIds,
        List<Long> manufacturerIds,
        List<Long> tagIds
) {
    public static ProductFilterRefs none() {
        return new ProductFilterRefs(null, null, null);
    }
}

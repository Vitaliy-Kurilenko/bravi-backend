package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import ua.com.bravi.bravi.seller.catalog.attributes.exception.InvalidAttributeRequestException;

import java.util.ArrayList;
import java.util.List;

/**
 * A gap-free zero-based ordering, which is the order the seller sees: the options of one attribute
 * in its value picker, and the attributes a category offers in its form. Pure functions over ids.
 */
public final class AttributeOrder {

    private static final String FIELD_SORT_ORDER = "sort_order";

    private final List<Long> ids;

    private AttributeOrder(List<Long> ids) {
        this.ids = ids;
    }

    /** Builds an ordering from ids already read in stored-position order. */
    public static AttributeOrder of(List<Long> orderedIds) {
        return new AttributeOrder(List.copyOf(orderedIds));
    }

    public int size() {
        return ids.size();
    }

    /** Position a newly added entry takes. */
    public int nextPosition() {
        return ids.size();
    }

    /** Ids in the order they take after moving {@code id} to {@code target}. */
    public List<Long> move(Long id, int target) {
        if (target < 0 || target >= ids.size()) {
            throw new InvalidAttributeRequestException(FIELD_SORT_ORDER,
                    "Position must be between 0 and " + (ids.size() - 1));
        }
        List<Long> moved = new ArrayList<>(ids);
        moved.remove(id);
        moved.add(target, id);
        return List.copyOf(moved);
    }

    /** Ids in the order they take after {@code removed} leaves the ordering. */
    public List<Long> without(Long removed) {
        return ids.stream()
                .filter(id -> !id.equals(removed))
                .toList();
    }
}

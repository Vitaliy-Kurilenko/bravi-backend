package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles the set of attributes a product in a given category may carry. A binding made on a
 * category also reaches every descendant, so "Material" is attached once to "Clothing" instead of to
 * each of its subcategories. The set is derived on every read rather than stored, which is why moving
 * a category under a new parent needs no data migration.
 */
public final class AttributeInheritance {

    private AttributeInheritance() {
    }

    /** An attribute offered by one category, as stored in the binding table. */
    public record Binding(Long categoryId, Long attributeId, Integer sortOrder) {
    }

    /** An attribute offered to a product, together with where the offer comes from. */
    public record EffectiveAttribute(Attribute attribute, AttributeSource source, Long sourceCategoryId) {
    }

    /**
     * Global attributes first, then bindings walked from the root down to the category itself, each
     * category's own bindings in their stored order. An attribute bound at several levels is listed
     * once, keeping the position where it first appears and the source closest to the product.
     *
     * @param categoryPathFromSelf the category and its ancestors, nearest first; empty for a product without a category
     */
    public static List<EffectiveAttribute> effective(List<Long> categoryPathFromSelf,
                                                     Collection<Attribute> globalAttributes,
                                                     Collection<Binding> bindings,
                                                     Map<Long, Attribute> attributesById) {
        Map<Long, EffectiveAttribute> effective = new LinkedHashMap<>();

        globalAttributes.stream()
                .sorted(Comparator.comparing(Attribute::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(attribute ->
                        effective.put(attribute.id(), new EffectiveAttribute(attribute, AttributeSource.GLOBAL, null)));

        Long leafCategoryId = categoryPathFromSelf.isEmpty() ? null : categoryPathFromSelf.getFirst();
        List<Long> pathFromRoot = new ArrayList<>(categoryPathFromSelf).reversed();

        for (Long categoryId : pathFromRoot) {
            bindings.stream()
                    .filter(binding -> categoryId.equals(binding.categoryId()))
                    .sorted(Comparator.comparing(Binding::sortOrder))
                    .forEach(binding -> {
                        Attribute attribute = attributesById.get(binding.attributeId());
                        if (attribute == null || attribute.scope() == AttributeScope.GLOBAL) {
                            return;
                        }
                        AttributeSource source = categoryId.equals(leafCategoryId)
                                ? AttributeSource.OWN
                                : AttributeSource.INHERITED;
                        effective.put(attribute.id(), new EffectiveAttribute(attribute, source, categoryId));
                    });
        }

        return List.copyOf(effective.values());
    }
}

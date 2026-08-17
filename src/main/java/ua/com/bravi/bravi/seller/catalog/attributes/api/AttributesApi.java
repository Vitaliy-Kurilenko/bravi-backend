package ua.com.bravi.bravi.seller.catalog.attributes.api;

import ua.com.bravi.bravi.seller.catalog.attributes.domain.Attribute;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeOption;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSearchQuery;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValue;

import java.util.List;

public interface AttributesApi {

    // Library of platform-provided definitions a store can adopt.

    List<AttributeTemplateView> listTemplates(Long storeId, String search);

    // Store-owned definitions.

    AttributePage search(Long storeId, AttributeSearchQuery query);

    AttributeView getByPublicId(Long storeId, String publicId);

    /** Creates a definition together with its options, so a picker is set up in one call. */
    AttributeView create(Long storeId, Attribute attribute, List<AttributeOption> options);

    void update(Long storeId, String publicId, Attribute patch);

    void delete(Long storeId, String publicId);

    // Options of a SELECT / MULTI_SELECT definition.

    AttributeOptionView addOption(Long storeId, String attributePublicId, AttributeOption option);

    /** Renames or repositions an option and returns the whole list in its resulting order. */
    List<AttributeOptionView> updateOption(Long storeId, String attributePublicId, String optionPublicId,
                                           String name, Integer sortOrder);

    void deleteOption(Long storeId, String attributePublicId, String optionPublicId);

    /** Free-text values already entered for this attribute, so the seller reuses them instead of retyping. */
    List<String> suggestValues(Long storeId, String attributePublicId, String search);

    // Category bindings. A binding also reaches every descendant category.

    /** Everything a product of this category may carry: global definitions, inherited ones, and its own. */
    List<CategoryAttributeView> listCategoryAttributes(Long storeId, String categoryPublicId);

    /**
     * Binds definitions to a category, adopting any named template the store does not own yet.
     * An attribute the category already offers, itself or through an ancestor, is left as it is.
     * Returns the category's resulting effective set.
     */
    List<CategoryAttributeView> bindToCategory(Long storeId, String categoryPublicId,
                                               List<String> attributePublicIds, List<String> templateCodes);

    List<CategoryAttributeView> moveBinding(Long storeId, String categoryPublicId, String attributePublicId,
                                            int sortOrder);

    void unbindFromCategory(Long storeId, String categoryPublicId, String attributePublicId);

    // Product values. Owned here because validation needs the definitions.

    List<ProductAttributeValueView> listProductValues(Long storeId, Long productId, Long categoryId);

    ProductAttributesView describeProductAttributes(Long storeId, Long productId, Long categoryId);

    /** Replaces every value of the product with the submitted set. */
    List<ProductAttributeValueView> replaceProductValues(Long storeId, Long productId, Long categoryId,
                                                         List<AttributeValue> values);

    /** Writes the submitted attributes and leaves the product's other values untouched. */
    List<ProductAttributeValueView> mergeProductValues(Long storeId, Long productId, Long categoryId,
                                                       List<AttributeValue> values);

    /** Reads a product's values back as submittable input, for copying them onto another product. */
    List<AttributeValue> exportProductValues(Long storeId, Long productId);
}

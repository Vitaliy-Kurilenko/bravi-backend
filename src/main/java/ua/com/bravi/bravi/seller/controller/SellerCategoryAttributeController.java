package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributesApi;
import ua.com.bravi.bravi.seller.controller.dto.in.CategoryAttributeBindRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.CategoryAttributeUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.CategoryAttributeResponse;
import ua.com.bravi.bravi.seller.controller.mapper.AttributeDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.shared.component.RequireStore;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/categories/{publicId}/attributes")
@Tag(name = "SellerCategoryAttributeController")
@RequireStore
public class SellerCategoryAttributeController {

    private final AttributesApi attributesApi;
    private final AttributeDtoMapper attributeDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "List category attributes",
            description = "Everything a product of this category may carry: global attributes, attributes "
                    + "inherited from parent categories, and the category's own. Each entry reports where "
                    + "the offer comes from, so an inherited one is edited on the ancestor that defines it.")
    @GetMapping
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public List<CategoryAttributeResponse> getCategoryAttributes(@PathVariable String publicId) {
        return attributeDtoMapper.toCategoryAttributeResponses(
                attributesApi.listCategoryAttributes(storeContext.get(), publicId));
    }

    @Operation(summary = "Bind attributes to category",
            description = "Offers the given attributes to products of this category and every subcategory. "
                    + "Templates named by code are copied into the store first if it does not own them yet. "
                    + "An attribute the category already offers, itself or through an ancestor, is left alone. "
                    + "Returns the category's resulting set.")
    @PostMapping
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public List<CategoryAttributeResponse> bindCategoryAttributes(
            @PathVariable String publicId,
            @Valid @RequestBody CategoryAttributeBindRequest request
    ) {
        return attributeDtoMapper.toCategoryAttributeResponses(attributesApi.bindToCategory(
                storeContext.get(), publicId, request.attributeIds(), request.templateCodes()));
    }

    @Operation(summary = "Reorder category attribute",
            description = "Moves an attribute within the category's own bindings and returns the resulting set")
    @PatchMapping("/{attributePublicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public List<CategoryAttributeResponse> moveCategoryAttribute(
            @PathVariable String publicId,
            @PathVariable String attributePublicId,
            @Valid @RequestBody CategoryAttributeUpdateRequest request
    ) {
        return attributeDtoMapper.toCategoryAttributeResponses(attributesApi.moveBinding(
                storeContext.get(), publicId, attributePublicId, request.sortOrder()));
    }

    @Operation(summary = "Unbind attribute from category",
            description = "Stops offering the attribute in this category. Values products already carry are "
                    + "kept. An inherited binding must be removed on the ancestor that defines it.")
    @DeleteMapping("/{attributePublicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> unbindCategoryAttribute(
            @PathVariable String publicId,
            @PathVariable String attributePublicId
    ) {
        attributesApi.unbindFromCategory(storeContext.get(), publicId, attributePublicId);
        return ResponseEntity.noContent().build();
    }
}

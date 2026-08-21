package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductsApi;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductTagsBulkRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductTagsReplaceRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductTagsBulkResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.TagResponse;
import ua.com.bravi.bravi.seller.controller.mapper.TagDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.seller.tags.domain.TagBulkMode;
import ua.com.bravi.bravi.shared.component.RequireStore;

import java.util.List;

/**
 * Pins the store's tags on its products. The dictionary itself is shared with every other taggable
 * aggregate and lives in {@code SellerTagController}; what belongs here is the product half, because
 * the routes go through {@code ProductsApi} — it is the products module that proves a product
 * belongs to the store before this module's tags are touched.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers")
@Tag(name = "SellerProductTagController")
@RequireStore
public class SellerProductTagController {

    private final ProductsApi productsApi;
    private final TagDtoMapper tagDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "List product tags", description = "Returns the tags pinned on one product")
    @GetMapping("/products/{publicId}/tags")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public List<TagResponse> listProductTags(@PathVariable String publicId) {
        return tagDtoMapper.toResponses(productsApi.listTags(storeContext.get(), publicId));
    }

    @Operation(summary = "Replace product tags",
            description = "Leaves the product carrying exactly the submitted tags. An entry may address an "
                    + "existing tag by id or name it; an unknown name creates the tag. An empty list clears them")
    @PutMapping("/products/{publicId}/tags")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public List<TagResponse> replaceProductTags(@PathVariable String publicId,
                                                @Valid @RequestBody ProductTagsReplaceRequest request) {
        return tagDtoMapper.toResponses(productsApi.replaceTags(storeContext.get(), publicId,
                tagDtoMapper.toDomains(request.tags())));
    }

    @Operation(summary = "Apply tags in bulk",
            description = "Adds, removes or replaces the same tags on many products. Removing never creates "
                    + "a tag: an unknown name has nothing to unpin")
    @PostMapping("/product-tags/bulk")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ProductTagsBulkResponse applyProductTagsBulk(@Valid @RequestBody ProductTagsBulkRequest request) {
        TagBulkMode mode = request.mode() != null ? request.mode() : TagBulkMode.ADD;
        int updated = productsApi.applyTagsBulk(storeContext.get(), request.productIds(),
                tagDtoMapper.toDomains(request.tags()), mode);
        return tagDtoMapper.toBulkResponse(updated);
    }
}

package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductsApi;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSortBy;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductImageAttachRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductImageUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductImageUploadUrlRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductImageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductImageUploadUrlResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductResponse;
import ua.com.bravi.bravi.seller.controller.mapper.ProductDtoMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/products")
@Tag(name = "SellerProductController")
@RequireStore
public class SellerProductController {

    private final ProductsApi productsApi;
    private final ProductDtoMapper productDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "Search products",
            description = "Returns a paginated, filtered and sorted list of the current store's products. "
                    + "has_active_discount keeps only products whose price a discount is shaping right now, or only those it is not.")
    @GetMapping
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ProductPageResponse searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(name = "category_ids", required = false) List<String> categoryIds,
            @RequestParam(name = "manufacturer_ids", required = false) List<String> manufacturerIds,
            @RequestParam(name = "stock_statuses", required = false) List<Long> stockStatuses,
            @RequestParam(name = "statuses", required = false) List<ProductStatus> statuses,
            @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @RequestParam(name = "created_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(name = "created_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(name = "has_active_discount", required = false) Boolean hasActiveDiscount,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        ProductSearchQuery query = new ProductSearchQuery(
                search, categoryIds, manufacturerIds, stockStatuses, statuses,
                minPrice, maxPrice, createdFrom, createdTo, hasActiveDiscount,
                sortBy == null ? null : ProductSortBy.fromParam(sortBy), sortOrder, page, limit
        );
        return productDtoMapper.toPageResponse(productsApi.search(storeContext.get(), query));
    }

    @Operation(summary = "Get product", description = "Returns a single product of the current store")
    @GetMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ProductResponse getProduct(@PathVariable String publicId) {
        return productDtoMapper.toResponse(productsApi.getByPublicId(storeContext.get(), publicId));
    }

    @Operation(summary = "Create product", description = "Creates a product in the current store")
    @PostMapping
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse body = productDtoMapper.toResponse(
                productsApi.create(storeContext.get(), productDtoMapper.toDomain(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Update product", description = "Partially updates a product of the current store")
    @PatchMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> updateProduct(
            @PathVariable String publicId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        productsApi.update(storeContext.get(), publicId, productDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete product", description = "Deletes a product of the current store with its images")
    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String publicId) {
        productsApi.delete(storeContext.get(), publicId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List product images", description = "Returns the image gallery of a product")
    @GetMapping("/{publicId}/images")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public List<ProductImageResponse> getImages(@PathVariable String publicId) {
        return productDtoMapper.toImageResponses(productsApi.listImages(storeContext.get(), publicId));
    }

    @Operation(summary = "Request image upload URL",
            description = "Returns a presigned URL to upload a product image directly to storage")
    @PostMapping("/{publicId}/images/upload-url")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ProductImageUploadUrlResponse requestImageUploadUrl(
            @PathVariable String publicId,
            @Valid @RequestBody ProductImageUploadUrlRequest request
    ) {
        return productDtoMapper.toUploadUrlResponse(
                productsApi.presignImageUpload(storeContext.get(), publicId, productDtoMapper.toUpload(request)));
    }

    @Operation(summary = "Attach product image",
            description = "Confirms a previously uploaded image and adds it to the end of the product gallery")
    @PostMapping("/{publicId}/images")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<ProductImageResponse> attachImage(
            @PathVariable String publicId,
            @Valid @RequestBody ProductImageAttachRequest request
    ) {
        ProductImageResponse body = productDtoMapper.toImageResponse(
                productsApi.confirmImage(storeContext.get(), publicId, request.storageKey()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Move product image",
            description = "Moves an image to a position and returns the whole gallery in its new order; "
                    + "position 0 makes the image the product's main one")
    @PatchMapping("/{publicId}/images/{imageId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public List<ProductImageResponse> updateImage(
            @PathVariable String publicId,
            @PathVariable Long imageId,
            @Valid @RequestBody ProductImageUpdateRequest request
    ) {
        return productDtoMapper.toImageResponses(
                productsApi.moveImage(storeContext.get(), publicId, imageId, request.sortOrder()));
    }

    @Operation(summary = "Delete product image", description = "Removes an image from the product gallery")
    @DeleteMapping("/{publicId}/images/{imageId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable String publicId,
            @PathVariable Long imageId
    ) {
        productsApi.deleteImage(storeContext.get(), publicId, imageId);
        return ResponseEntity.noContent().build();
    }
}

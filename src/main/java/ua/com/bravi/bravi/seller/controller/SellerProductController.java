package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageContent;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageUpload;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductsApi;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSortBy;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductImageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductResponse;
import ua.com.bravi.bravi.seller.controller.mapper.ProductDtoMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;

import java.io.IOException;
import java.io.UncheckedIOException;
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
            description = "Returns a paginated, filtered and sorted list of the current store's products")
    @GetMapping
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ProductPageResponse searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(name = "category_ids", required = false) List<Long> categoryIds,
            @RequestParam(name = "manufacturer_ids", required = false) List<Long> manufacturerIds,
            @RequestParam(name = "stock_statuses", required = false) List<Long> stockStatuses,
            @RequestParam(name = "statuses", required = false) List<ProductStatus> statuses,
            @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @RequestParam(name = "created_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(name = "created_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        ProductSearchQuery query = new ProductSearchQuery(
                search, categoryIds, manufacturerIds, stockStatuses, statuses,
                minPrice, maxPrice, createdFrom, createdTo,
                sortBy == null ? null : ProductSortBy.fromParam(sortBy), sortOrder, page, limit
        );
        return productDtoMapper.toPageResponse(productsApi.search(storeContext.get(), query));
    }

    @Operation(summary = "Get product", description = "Returns a single product of the current store")
    @GetMapping("/{productId}")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return productDtoMapper.toResponse(productsApi.getById(storeContext.get(), productId));
    }

    @Operation(summary = "Create product", description = "Creates a product in the current store")
    @PostMapping
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Long storeId = storeContext.get();
        Long productId = productsApi.create(storeId, productDtoMapper.toDomain(request));
        ProductResponse body = productDtoMapper.toResponse(productsApi.getById(storeId, productId));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Update product", description = "Partially updates a product of the current store")
    @PatchMapping("/{productId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        productsApi.update(storeContext.get(), productId, productDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete product", description = "Deletes a product of the current store with its images")
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productsApi.delete(storeContext.get(), productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List product images", description = "Returns the image gallery of a product")
    @GetMapping("/{productId}/images")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public List<ProductImageResponse> getImages(@PathVariable Long productId) {
        return productDtoMapper.toImageResponses(productsApi.listImages(storeContext.get(), productId));
    }

    @Operation(summary = "Upload product image", description = "Adds an image to the product gallery")
    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<ProductImageResponse> addImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "is_primary", defaultValue = "false") boolean primary
    ) {
        ProductImageResponse body = productDtoMapper.toImageResponse(
                productsApi.addImage(storeContext.get(), productId, toUpload(file, primary)));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Replace product image", description = "Replaces the file of an existing product image")
    @PutMapping(value = "/{productId}/images/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ProductImageResponse replaceImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @RequestParam("file") MultipartFile file
    ) {
        return productDtoMapper.toImageResponse(
                productsApi.replaceImage(storeContext.get(), productId, imageId, toUpload(file, false)));
    }

    @Operation(summary = "Get product image content", description = "Streams the binary content of a product image")
    @GetMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ResponseEntity<byte[]> getImageContent(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        ImageContent content = productsApi.loadImageContent(storeContext.get(), productId, imageId);
        MediaType mediaType = content.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(content.contentType());
        return ResponseEntity.ok().contentType(mediaType).body(content.content());
    }

    @Operation(summary = "Delete product image", description = "Removes an image from the product gallery")
    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        productsApi.deleteImage(storeContext.get(), productId, imageId);
        return ResponseEntity.noContent().build();
    }

    private static ImageUpload toUpload(MultipartFile file, boolean primary) {
        try {
            return new ImageUpload(file.getBytes(), file.getContentType(), file.getOriginalFilename(),
                    file.getSize(), primary);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}

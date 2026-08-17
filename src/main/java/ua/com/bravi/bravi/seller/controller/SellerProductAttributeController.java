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
import ua.com.bravi.bravi.seller.controller.dto.in.ProductAttributesBulkRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductAttributesCopyRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductAttributesReplaceRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductAttributeValueResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductAttributesResponse;
import ua.com.bravi.bravi.seller.controller.mapper.AttributeDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.shared.component.RequireStore;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers")
@Tag(name = "SellerProductAttributeController")
@RequireStore
public class SellerProductAttributeController {

    private final ProductsApi productsApi;
    private final AttributeDtoMapper attributeDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "Get product attributes",
            description = "Returns the attributes the product's category offers together with the values the "
                    + "product currently carries. A value whose attribute is no longer offered is still "
                    + "reported, flagged as not offered.")
    @GetMapping("/products/{publicId}/attributes")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ProductAttributesResponse getProductAttributes(@PathVariable String publicId) {
        return attributeDtoMapper.toResponse(productsApi.describeAttributes(storeContext.get(), publicId));
    }

    @Operation(summary = "Replace product attributes",
            description = "Sets the product's whole set of characteristic values; anything absent is removed")
    @PutMapping("/products/{publicId}/attributes")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public List<ProductAttributeValueResponse> replaceProductAttributes(
            @PathVariable String publicId,
            @Valid @RequestBody ProductAttributesReplaceRequest request
    ) {
        return attributeDtoMapper.toValueResponses(productsApi.replaceAttributes(
                storeContext.get(), publicId, attributeDtoMapper.toValueDomains(request.attributes())));
    }

    @Operation(summary = "Copy product attributes",
            description = "Replaces this product's characteristic values with those of another product of the store")
    @PostMapping("/products/{publicId}/attributes/copy-from")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public List<ProductAttributeValueResponse> copyProductAttributes(
            @PathVariable String publicId,
            @Valid @RequestBody ProductAttributesCopyRequest request
    ) {
        return attributeDtoMapper.toValueResponses(
                productsApi.copyAttributesFrom(storeContext.get(), publicId, request.productId()));
    }

    @Operation(summary = "Apply attributes to many products",
            description = "Writes the same characteristic values onto every listed product, leaving their "
                    + "other attributes untouched")
    @PostMapping("/product-attributes/bulk")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public Map<String, Integer> applyProductAttributesBulk(
            @Valid @RequestBody ProductAttributesBulkRequest request
    ) {
        int updated = productsApi.applyAttributesBulk(storeContext.get(), request.productIds(),
                attributeDtoMapper.toValueDomains(request.attributes()));
        return Map.of("updated", updated);
    }
}

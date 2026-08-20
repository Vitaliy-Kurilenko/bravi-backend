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
import ua.com.bravi.bravi.seller.controller.dto.in.ProductDiscountsBulkRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ProductDiscountsReplaceRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductDiscountResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ProductDiscountsBulkResponse;
import ua.com.bravi.bravi.seller.controller.mapper.DiscountDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.shared.component.RequireStore;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers")
@Tag(name = "SellerProductDiscountController")
@RequireStore
public class SellerProductDiscountController {

    private final ProductsApi productsApi;
    private final DiscountDtoMapper discountDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "Get product discounts",
            description = "Returns the product's discount schedule, earliest period first. The status of "
                    + "each entry is derived from its period and the current time.")
    @GetMapping("/products/{publicId}/discounts")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public List<ProductDiscountResponse> getProductDiscounts(@PathVariable String publicId) {
        return discountDtoMapper.toResponses(productsApi.listDiscounts(storeContext.get(), publicId));
    }

    @Operation(summary = "Replace product discounts",
            description = "Replaces the product's whole schedule in one step. An entry carrying a public id "
                    + "keeps that discount and its creation time, an entry without one is created, and a "
                    + "stored discount left out is removed — which is how a running discount is stopped "
                    + "early. Only the writable fields are read; status and timestamps are ignored. "
                    + "Periods of one product may not overlap.")
    @PutMapping("/products/{publicId}/discounts")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public List<ProductDiscountResponse> replaceProductDiscounts(
            @PathVariable String publicId,
            @Valid @RequestBody ProductDiscountsReplaceRequest request) {
        return discountDtoMapper.toResponses(productsApi.replaceDiscounts(storeContext.get(), publicId,
                discountDtoMapper.toSubmitted(request.discounts())));
    }

    @Operation(summary = "Apply one discount to many products",
            description = "Adds the same discount to every listed product. A product whose schedule already "
                    + "covers the period, or whose price a fixed amount would not stay below, is reported "
                    + "as skipped instead of failing the whole request.")
    @PostMapping("/product-discounts/bulk")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ProductDiscountsBulkResponse applyProductDiscountsBulk(
            @Valid @RequestBody ProductDiscountsBulkRequest request) {
        return discountDtoMapper.toBulkResponse(productsApi.applyDiscountsBulk(storeContext.get(),
                request.productIds(), discountDtoMapper.toDomain(request)));
    }
}

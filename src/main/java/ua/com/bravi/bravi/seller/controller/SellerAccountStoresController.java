package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.component.SellerAccountResolver;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;

import java.util.List;

/**
 * Account-scoped store listing for the current seller. The seller-context interceptor resolves the
 * path {@code accountPublicId} into {@link ua.com.bravi.bravi.access.api.CurrentAccountHolder}
 * (validating membership) before {@code @PreAuthorize} runs.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts/{accountPublicId}/seller/stores")
@Tag(name = "SellerAccountStoresController")
public class SellerAccountStoresController {

    private final StoresApi storesApi;
    private final SellerAccountResolver sellerAccountResolver;
    private final StoreDtoMapper storeDtoMapper;

    @Operation(summary = "List account stores", description = "Returns all stores of the account")
    @GetMapping
    @PreAuthorize("hasPermission('STORE', 'READ')")
    public List<StoreResponse> listStores(@PathVariable String accountPublicId) {
        Long accountId = sellerAccountResolver.resolveSellerAccountId(accountPublicId);
        return storesApi.getStoresByAccountId(accountId).stream()
                .map(storeDtoMapper::toResponse)
                .toList();
    }
}

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
import ua.com.bravi.bravi.seller.component.SellerAccountResolver;
import ua.com.bravi.bravi.seller.controller.dto.in.LogoUploadUrlRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.LogoUploadUrlResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.seller.controller.mapper.StoreLogoDtoMapper;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.shared.component.RequireStore;

import java.util.List;

/**
 * The {@code /sellers/stores} resource. The collection {@code GET /sellers/stores} lists the current
 * seller account's stores (account-scoped, from {@code X-Account-Id}); the item
 * {@code /sellers/stores/{storeId}} manages a single store (get/patch + logo), with the store-context
 * interceptor resolving and ownership-validating {@code {storeId}} into {@link StoreContext}
 * ({@code @RequireStore} on the item methods). Store creation happens during onboarding
 * ({@link SellerOnboardingController}), not here.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/stores")
@Tag(name = "SellerStoresController")
public class SellerStoresController {

    private final StoresApi storesApi;
    private final StoreDtoMapper storeDtoMapper;
    private final StoreLogoDtoMapper storeLogoDtoMapper;
    private final StoreContext storeContext;
    private final SellerAccountResolver sellerAccountResolver;

    @Operation(summary = "List stores", description = "Returns all stores of the current seller account")
    @GetMapping
    @PreAuthorize("hasPermission('STORE', 'READ')")
    public List<StoreResponse> listStores() {
        Long accountId = sellerAccountResolver.resolveSellerAccountId();
        return storesApi.getStoresByAccountId(accountId).stream()
                .map(storeDtoMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Get store", description = "Returns a single store of the current seller")
    @GetMapping("/{storeId}")
    @RequireStore
    @PreAuthorize("hasPermission('STORE', 'READ')")
    public StoreResponse getStore(@PathVariable String storeId) {
        return storesApi.getStoreById(storeContext.get())
                .map(storeDtoMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Store not found"));
    }

    @Operation(summary = "Update store", description = "Partially updates the store; a non-null logo_storage_key attaches an uploaded logo")
    @PatchMapping("/{storeId}")
    @RequireStore
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public ResponseEntity<Void> updateStore(@PathVariable String storeId,
                                            @Valid @RequestBody StoreUpdateRequest request) {
        Long id = storeContext.get();
        storesApi.updateStore(id, storeDtoMapper.toDomain(request));
        if (request.logoStorageKey() != null) {
            storesApi.confirmLogo(id, request.logoStorageKey());
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request logo upload URL", description = "Returns a presigned URL to upload the store logo directly to storage")
    @PostMapping("/{storeId}/logo/upload-url")
    @RequireStore
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public LogoUploadUrlResponse requestLogoUploadUrl(@PathVariable String storeId,
                                                      @Valid @RequestBody LogoUploadUrlRequest request) {
        return storeLogoDtoMapper.toResponse(
                storesApi.presignLogoUpload(storeContext.get(), storeLogoDtoMapper.toUpload(request)));
    }

    @Operation(summary = "Delete logo", description = "Removes the store logo")
    @DeleteMapping("/{storeId}/logo")
    @RequireStore
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public StoreResponse deleteLogo(@PathVariable String storeId) {
        return storeDtoMapper.toResponse(storesApi.removeLogo(storeContext.get()));
    }
}

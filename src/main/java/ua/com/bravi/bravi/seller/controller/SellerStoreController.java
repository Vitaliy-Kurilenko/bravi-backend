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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.controller.dto.in.LogoUploadUrlRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.LogoUploadUrlResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.seller.controller.mapper.StoreLogoDtoMapper;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;
import ua.com.bravi.bravi.shared.component.RequireStore;

/**
 * Post-onboarding day-to-day store management for the current seller. Store
 * creation happens during onboarding ({@link SellerOnboardingController}), not here.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stores/{storePublicId}")
@Tag(name = "SellerStoreController")
@RequireStore
public class SellerStoreController {

    private final StoresApi storesApi;
    private final StoreDtoMapper storeDtoMapper;
    private final StoreLogoDtoMapper storeLogoDtoMapper;
    private final CurrentStoreHolder currentStoreHolder;

    @Operation(summary = "Get store", description = "Returns the current user's store")
    @GetMapping
    @PreAuthorize("hasPermission('STORE', 'READ')")
    public StoreResponse getStore() {
        Long storeId = currentStoreHolder.get();
        return storesApi.getStoreById(storeId)
                .map(storeDtoMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Store not found"));
    }

    @Operation(summary = "Update store", description = "Partially updates the current user's store; a non-null logo_storage_key attaches an uploaded logo")
    @PatchMapping
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public ResponseEntity<Void> updateStore(@Valid @RequestBody StoreUpdateRequest request) {
        Long storeId = currentStoreHolder.get();
        storesApi.updateStore(storeId, storeDtoMapper.toDomain(request));
        if (request.logoStorageKey() != null) {
            storesApi.confirmLogo(storeId, request.logoStorageKey());
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request logo upload URL", description = "Returns a presigned URL to upload the store logo directly to storage")
    @PostMapping("/logo/upload-url")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public LogoUploadUrlResponse requestLogoUploadUrl(@Valid @RequestBody LogoUploadUrlRequest request) {
        return storeLogoDtoMapper.toResponse(
                storesApi.presignLogoUpload(currentStoreHolder.get(), storeLogoDtoMapper.toUpload(request)));
    }

    @Operation(summary = "Delete logo", description = "Removes the current store logo")
    @DeleteMapping("/logo")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public StoreResponse deleteLogo() {
        return storeDtoMapper.toResponse(storesApi.removeLogo(currentStoreHolder.get()));
    }
}

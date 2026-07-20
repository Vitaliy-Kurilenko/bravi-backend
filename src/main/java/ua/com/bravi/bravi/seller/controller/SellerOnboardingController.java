package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.SellerOnboardingService;
import ua.com.bravi.bravi.seller.controller.dto.in.LogoUploadUrlRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OnboardingContactsRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OnboardingSettingsRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OnboardingStorePatchRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OnboardingStoreRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.LogoUploadUrlResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.OnboardingStateResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreContactResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.controller.mapper.OnboardingDtoMapper;
import ua.com.bravi.bravi.seller.controller.mapper.StoreContactDtoMapper;

import java.util.List;

/**
 * Seller onboarding surface (spec §5). Authenticated; each method requires the STORE permission
 * of the current account, and the path {@code accountId} must match the caller's active account.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts/{accountId}/seller/onboarding")
@Tag(name = "SellerOnboardingController")
public class SellerOnboardingController {

    private final SellerOnboardingService onboardingService;
    private final OnboardingDtoMapper onboardingDtoMapper;
    private final StoreContactDtoMapper storeContactDtoMapper;

    @Operation(summary = "Onboarding state", description = "Current onboarding progress for the account")
    @GetMapping
    @PreAuthorize("hasPermission('STORE', 'READ')")
    public OnboardingStateResponse getState(@PathVariable String accountId) {
        return onboardingService.getState(accountId);
    }

    @Operation(summary = "Create draft store", description = "Creates the DRAFT store, default settings and manual channel")
    @PostMapping("/store")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public ResponseEntity<StoreResponse> createStore(@PathVariable String accountId,
                                                     @Valid @RequestBody OnboardingStoreRequest request) {
        StoreResponse response = onboardingService.createStore(accountId, onboardingDtoMapper.toDraft(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update draft store", description = "Partially updates the DRAFT store; a non-null logo_storage_key attaches an uploaded logo")
    @PatchMapping("/store")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public ResponseEntity<Void> updateStore(@PathVariable String accountId,
                                            @RequestBody OnboardingStorePatchRequest request) {
        onboardingService.updateStore(accountId, onboardingDtoMapper.toDraft(request), request.logoStorageKey());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request logo upload URL", description = "Returns a presigned URL to upload the store logo directly to storage")
    @PostMapping("/store/logo/upload-url")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public LogoUploadUrlResponse requestLogoUploadUrl(@PathVariable String accountId,
                                                      @Valid @RequestBody LogoUploadUrlRequest request) {
        return onboardingService.presignLogoUpload(accountId, request);
    }

    @Operation(summary = "Delete logo", description = "Removes the DRAFT store logo")
    @DeleteMapping("/store/logo")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public StoreResponse deleteLogo(@PathVariable String accountId) {
        return onboardingService.removeLogo(accountId);
    }

    @Operation(summary = "Update store settings", description = "Partially updates the store settings")
    @PatchMapping("/store/settings")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public ResponseEntity<Void> updateSettings(@PathVariable String accountId,
                                               @RequestBody OnboardingSettingsRequest request) {
        onboardingService.updateSettings(accountId, onboardingDtoMapper.toSettings(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Replace store contacts", description = "Replaces the full set of store contacts")
    @PutMapping("/store/contacts")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public List<StoreContactResponse> replaceContacts(@PathVariable String accountId,
                                                      @Valid @RequestBody OnboardingContactsRequest request) {
        return onboardingService.replaceContacts(accountId,
                storeContactDtoMapper.toDomainFromCreate(request.contacts()));
    }

    @Operation(summary = "Complete onboarding", description = "Finalizes onboarding: account ACTIVE, store ACTIVE")
    @PostMapping("/complete")
    @PreAuthorize("hasPermission('STORE', 'WRITE')")
    public OnboardingStateResponse complete(@PathVariable String accountId) {
        return onboardingService.complete(accountId);
    }
}

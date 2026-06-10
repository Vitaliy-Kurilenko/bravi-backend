package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.stores.api.StoresApi;
import ua.com.bravi.bravi.stores.api.CurrentStoreHolder;
import ua.com.bravi.bravi.shared.component.PermitNoStore;
import ua.com.bravi.bravi.shared.component.RequireStore;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/stores")
@PreAuthorize("hasAuthority('role_seller')")
@Tag(name = "SellerStoreController")
@RequireStore
public class SellerStoreController {

    private final StoresApi storesApi;
    private final StoreDtoMapper storeDtoMapper;
    private final InvocationContext invocationContext;
    private final CurrentStoreHolder currentStoreHolder;

    @Operation(summary = "Get store", description = "Returns the current user's store")
    @GetMapping
    public StoreResponse getStore() {
        System.out.println("Test");
        Long storeId = currentStoreHolder.get();
        return storesApi.getStoreById(storeId)
                .map(storeDtoMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Store not found"));
    }

    @Operation(summary = "Create store", description = "Creates a store for the current user")
    @PostMapping
    @PermitNoStore
    public ResponseEntity<Void> createStore(@Valid @RequestBody StoreCreateRequest request) {
        Long storeId = storesApi.createStore(invocationContext.getUserId(), storeDtoMapper.toDomain(request));
        currentStoreHolder.set(storeId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Update store", description = "Partially updates the current user's store")
    @PatchMapping
    public ResponseEntity<Void> updateStore(@Valid @RequestBody StoreUpdateRequest request) {
        storesApi.updateStore(currentStoreHolder.get(), storeDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }
}

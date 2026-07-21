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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.seller.controller.dto.in.ManufacturerCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ManufacturerUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ManufacturerResponse;
import ua.com.bravi.bravi.seller.controller.mapper.ManufacturerDtoMapper;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stores/{storePublicId}/manufacturers")
@Tag(name = "SellerManufacturerController")
@RequireStore
public class SellerManufacturerController {

    private final ManufacturersApi manufacturersApi;
    private final ManufacturerDtoMapper manufacturerDtoMapper;
    private final CurrentStoreHolder currentStoreHolder;

    @Operation(summary = "Get manufacturers", description = "Returns all manufacturers of the current user's store")
    @GetMapping
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public List<ManufacturerResponse> getManufacturers() {
        return manufacturerDtoMapper.toResponses(manufacturersApi.findByStoreId(currentStoreHolder.get()));
    }

    @Operation(summary = "Get manufacturer", description = "Returns a manufacturer of the current user's store")
    @GetMapping("/{manufacturerId}")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ManufacturerResponse getManufacturer(@PathVariable Long manufacturerId) {
        return manufacturerDtoMapper.toResponse(manufacturersApi.getById(currentStoreHolder.get(), manufacturerId));
    }

    @Operation(summary = "Create manufacturer", description = "Creates a manufacturer in the current user's store")
    @PostMapping
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> createManufacturer(@Valid @RequestBody ManufacturerCreateRequest request) {
        manufacturersApi.create(currentStoreHolder.get(), manufacturerDtoMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Update manufacturer", description = "Partially updates a manufacturer of the current user's store")
    @PatchMapping("/{manufacturerId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> updateManufacturer(
            @PathVariable Long manufacturerId,
            @Valid @RequestBody ManufacturerUpdateRequest request
    ) {
        manufacturersApi.update(currentStoreHolder.get(), manufacturerId, manufacturerDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete manufacturer", description = "Deletes a manufacturer of the current user's store")
    @DeleteMapping("/{manufacturerId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> deleteManufacturer(@PathVariable Long manufacturerId) {
        manufacturersApi.delete(currentStoreHolder.get(), manufacturerId);
        return ResponseEntity.noContent().build();
    }
}

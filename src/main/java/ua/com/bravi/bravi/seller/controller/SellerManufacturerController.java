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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSearchQuery;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSortBy;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerStatus;
import ua.com.bravi.bravi.seller.controller.dto.in.ManufacturerCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.ManufacturerUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.ManufacturerPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.ManufacturerResponse;
import ua.com.bravi.bravi.seller.controller.mapper.ManufacturerDtoMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/manufacturers")
@Tag(name = "SellerManufacturerController")
@RequireStore
public class SellerManufacturerController {

    private final ManufacturersApi manufacturersApi;
    private final ManufacturerDtoMapper manufacturerDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "Search manufacturers",
            description = "Returns a paginated, filtered and sorted list of the current store's manufacturers")
    @GetMapping
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ManufacturerPageResponse getManufacturers(
            @RequestParam(required = false) String search,
            @RequestParam(name = "statuses", required = false) List<ManufacturerStatus> statuses,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        ManufacturerSearchQuery query = new ManufacturerSearchQuery(
                search, statuses,
                sortBy == null ? null : ManufacturerSortBy.fromParam(sortBy), sortOrder, page, limit
        );
        return manufacturerDtoMapper.toPageResponse(manufacturersApi.search(storeContext.get(), query));
    }

    @Operation(summary = "Get manufacturer", description = "Returns a manufacturer of the current user's store")
    @GetMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public ManufacturerResponse getManufacturer(@PathVariable String publicId) {
        return manufacturerDtoMapper.toResponse(manufacturersApi.getByPublicId(storeContext.get(), publicId));
    }

    @Operation(summary = "Create manufacturer", description = "Creates a manufacturer in the current user's store")
    @PostMapping
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<ManufacturerResponse> createManufacturer(@Valid @RequestBody ManufacturerCreateRequest request) {
        ManufacturerResponse created = manufacturerDtoMapper.toResponse(
                manufacturersApi.create(storeContext.get(), manufacturerDtoMapper.toDomain(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update manufacturer", description = "Partially updates a manufacturer of the current user's store")
    @PatchMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> updateManufacturer(
            @PathVariable String publicId,
            @Valid @RequestBody ManufacturerUpdateRequest request
    ) {
        manufacturersApi.update(storeContext.get(), publicId, manufacturerDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete manufacturer", description = "Deletes a manufacturer of the current user's store")
    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> deleteManufacturer(@PathVariable String publicId) {
        manufacturersApi.delete(storeContext.get(), publicId);
        return ResponseEntity.noContent().build();
    }
}

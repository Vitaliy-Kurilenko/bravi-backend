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
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributesApi;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSearchQuery;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSortBy;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeStatus;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;
import ua.com.bravi.bravi.seller.controller.dto.in.AttributeCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.AttributeOptionRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.AttributeOptionUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.AttributeUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.AttributeOptionResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.AttributePageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.AttributeResponse;
import ua.com.bravi.bravi.seller.controller.mapper.AttributeDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.component.RequireStore;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/attributes")
@Tag(name = "SellerAttributeController")
@RequireStore
public class SellerAttributeController {

    private final AttributesApi attributesApi;
    private final AttributeDtoMapper attributeDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "Search attributes",
            description = "Returns a paginated, filtered and sorted list of the current store's attribute definitions")
    @GetMapping
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public AttributePageResponse getAttributes(
            @RequestParam(required = false) String search,
            @RequestParam(name = "value_types", required = false) List<AttributeValueType> valueTypes,
            @RequestParam(name = "scopes", required = false) List<AttributeScope> scopes,
            @RequestParam(name = "statuses", required = false) List<AttributeStatus> statuses,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "ASC") SortOrder sortOrder,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        AttributeSearchQuery query = new AttributeSearchQuery(
                search, valueTypes, scopes, statuses,
                sortBy == null ? null : AttributeSortBy.fromParam(sortBy), sortOrder, page, limit
        );
        return attributeDtoMapper.toPageResponse(attributesApi.search(storeContext.get(), query));
    }

    @Operation(summary = "Get attribute", description = "Returns an attribute definition of the current user's store")
    @GetMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public AttributeResponse getAttribute(@PathVariable String publicId) {
        return attributeDtoMapper.toResponse(attributesApi.getByPublicId(storeContext.get(), publicId));
    }

    @Operation(summary = "Create attribute",
            description = "Creates an attribute definition together with its options. A CATEGORY-scoped "
                    + "attribute reaches products once bound to a category; a GLOBAL one reaches every product.")
    @PostMapping
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<AttributeResponse> createAttribute(@Valid @RequestBody AttributeCreateRequest request) {
        AttributeResponse created = attributeDtoMapper.toResponse(attributesApi.create(
                storeContext.get(),
                attributeDtoMapper.toDomain(request),
                attributeDtoMapper.toOptionDomains(request.options())));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update attribute",
            description = "Partially updates an attribute definition. Code and value type are fixed after creation.")
    @PatchMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> updateAttribute(
            @PathVariable String publicId,
            @Valid @RequestBody AttributeUpdateRequest request
    ) {
        attributesApi.update(storeContext.get(), publicId, attributeDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete attribute",
            description = "Deletes an attribute definition and its category bindings. Rejected with 409 while "
                    + "products still carry values for it.")
    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> deleteAttribute(@PathVariable String publicId) {
        attributesApi.delete(storeContext.get(), publicId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add attribute option",
            description = "Appends a choice to a SELECT or MULTI_SELECT attribute")
    @PostMapping("/{publicId}/options")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<AttributeOptionResponse> addAttributeOption(
            @PathVariable String publicId,
            @Valid @RequestBody AttributeOptionRequest request
    ) {
        AttributeOptionResponse created = attributeDtoMapper.toResponse(attributesApi.addOption(
                storeContext.get(), publicId, attributeDtoMapper.toDomain(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update attribute option",
            description = "Renames or repositions an option and returns the whole list in its resulting order")
    @PatchMapping("/{publicId}/options/{optionPublicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public List<AttributeOptionResponse> updateAttributeOption(
            @PathVariable String publicId,
            @PathVariable String optionPublicId,
            @Valid @RequestBody AttributeOptionUpdateRequest request
    ) {
        return attributeDtoMapper.toOptionResponses(attributesApi.updateOption(
                storeContext.get(), publicId, optionPublicId, request.name(), request.sortOrder()));
    }

    @Operation(summary = "Delete attribute option",
            description = "Deletes a choice. Rejected with 409 while products still select it.")
    @DeleteMapping("/{publicId}/options/{optionPublicId}")
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE')")
    public ResponseEntity<Void> deleteAttributeOption(
            @PathVariable String publicId,
            @PathVariable String optionPublicId
    ) {
        attributesApi.deleteOption(storeContext.get(), publicId, optionPublicId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Suggest attribute values",
            description = "Values already entered for a TEXT attribute elsewhere in the store, so they are "
                    + "reused instead of retyped. Empty for the other value types.")
    @GetMapping("/{publicId}/value-suggestions")
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public List<String> getAttributeValueSuggestions(
            @PathVariable String publicId,
            @RequestParam(required = false) String search
    ) {
        return attributesApi.suggestValues(storeContext.get(), publicId, search);
    }
}

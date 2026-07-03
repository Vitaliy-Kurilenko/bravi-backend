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
import ua.com.bravi.bravi.seller.controller.dto.in.StoreContactCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreContactUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreContactResponse;
import ua.com.bravi.bravi.seller.controller.mapper.StoreContactDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.seller.stores.contacts.api.StoreContactsApi;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/stores/contacts")
@PreAuthorize("hasAuthority('role_seller')")
@Tag(name = "SellerStoreContactController")
@RequireStore
public class SellerStoreContactController {

    private final StoreContactsApi storeContactsApi;
    private final StoreContactDtoMapper storeContactDtoMapper;
    private final CurrentStoreHolder currentStoreHolder;

    @Operation(summary = "Get store contacts", description = "Returns all contacts of the current user's store")
    @GetMapping
    public List<StoreContactResponse> getContacts() {
        return storeContactDtoMapper.toResponses(storeContactsApi.findByStoreId(currentStoreHolder.get()));
    }

    @Operation(summary = "Add store contacts", description = "Adds one or more contacts to the current user's store")
    @PostMapping
    public ResponseEntity<Void> addContacts(
            @Valid @RequestBody List<@Valid StoreContactCreateRequest> requests
    ) {
        storeContactsApi.addContacts(currentStoreHolder.get(), storeContactDtoMapper.toDomainFromCreate(requests));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Update store contact", description = "Partially updates a contact of the current user's store")
    @PatchMapping("/{contactId}")
    public ResponseEntity<Void> updateContact(
            @PathVariable Long contactId,
            @Valid @RequestBody StoreContactUpdateRequest request
    ) {
        storeContactsApi.updateContact(currentStoreHolder.get(), contactId, storeContactDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete store contact", description = "Deletes a contact of the current user's store")
    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long contactId) {
        storeContactsApi.deleteContact(currentStoreHolder.get(), contactId);
        return ResponseEntity.noContent().build();
    }
}

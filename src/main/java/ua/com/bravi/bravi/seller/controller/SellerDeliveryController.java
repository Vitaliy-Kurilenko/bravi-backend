package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.controller.dto.in.DeliveryMethodConfigRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.DeliveryMethodDefinitionResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreDeliveryMethodResponse;
import ua.com.bravi.bravi.seller.controller.mapper.DeliveryDtoMapper;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;
import ua.com.bravi.bravi.seller.stores.delivery.api.DeliveryApi;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/stores/delivery")
@PreAuthorize("hasAuthority('role_seller')")
@Tag(name = "SellerDeliveryController")
@RequireStore
public class SellerDeliveryController {

    private final DeliveryApi deliveryApi;
    private final DeliveryDtoMapper deliveryDtoMapper;
    private final CurrentStoreHolder currentStoreHolder;

    @Operation(summary = "List available delivery methods",
            description = "Returns the catalog of delivery methods implemented in the system")
    @GetMapping("/available")
    public List<DeliveryMethodDefinitionResponse> getAvailableMethods() {
        return deliveryDtoMapper.toDefinitionResponses(deliveryApi.listAvailableMethods());
    }

    @Operation(summary = "List store delivery methods",
            description = "Returns delivery methods connected to the current user's store")
    @GetMapping
    public List<StoreDeliveryMethodResponse> getMethods() {
        return deliveryDtoMapper.toResponses(deliveryApi.findByStoreId(currentStoreHolder.get()));
    }

    @Operation(summary = "Enable delivery method",
            description = "Connects and configures a delivery method for the current user's store (idempotent)")
    @PutMapping("/{methodCode}")
    public ResponseEntity<Void> enableMethod(
            @PathVariable String methodCode,
            @RequestBody(required = false) DeliveryMethodConfigRequest request
    ) {
        deliveryApi.enableMethod(currentStoreHolder.get(), methodCode, configOf(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update delivery method config",
            description = "Updates the configuration of a connected delivery method")
    @PatchMapping("/{methodCode}")
    public ResponseEntity<Void> updateMethodConfig(
            @PathVariable String methodCode,
            @RequestBody DeliveryMethodConfigRequest request
    ) {
        deliveryApi.updateMethodConfig(currentStoreHolder.get(), methodCode, configOf(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Disable delivery method",
            description = "Disables a connected delivery method; its configuration is preserved")
    @DeleteMapping("/{methodCode}")
    public ResponseEntity<Void> disableMethod(@PathVariable String methodCode) {
        deliveryApi.disableMethod(currentStoreHolder.get(), methodCode);
        return ResponseEntity.noContent().build();
    }

    private Map<String, String> configOf(DeliveryMethodConfigRequest request) {
        return request == null || request.config() == null ? Map.of() : request.config();
    }
}

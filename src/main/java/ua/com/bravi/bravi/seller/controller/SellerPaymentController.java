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
import ua.com.bravi.bravi.seller.controller.dto.in.PaymentMethodConfigRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.PaymentMethodDefinitionResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.StorePaymentMethodResponse;
import ua.com.bravi.bravi.seller.controller.mapper.PaymentDtoMapper;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.stores.api.CurrentStoreHolder;
import ua.com.bravi.bravi.stores.payments.api.PaymentsApi;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/stores/payments")
@PreAuthorize("hasAuthority('role_seller')")
@Tag(name = "SellerPaymentController")
@RequireStore
public class SellerPaymentController {

    private final PaymentsApi paymentsApi;
    private final PaymentDtoMapper paymentDtoMapper;
    private final CurrentStoreHolder currentStoreHolder;

    @Operation(summary = "List available payment methods",
            description = "Returns the catalog of payment methods implemented in the system")
    @GetMapping("/available")
    public List<PaymentMethodDefinitionResponse> getAvailableMethods() {
        return paymentDtoMapper.toDefinitionResponses(paymentsApi.listAvailableMethods());
    }

    @Operation(summary = "List store payment methods",
            description = "Returns payment methods connected to the current user's store")
    @GetMapping
    public List<StorePaymentMethodResponse> getMethods() {
        return paymentDtoMapper.toResponses(paymentsApi.findByStoreId(currentStoreHolder.get()));
    }

    @Operation(summary = "Enable payment method",
            description = "Connects and configures a payment method for the current user's store (idempotent)")
    @PutMapping("/{methodCode}")
    public ResponseEntity<Void> enableMethod(
            @PathVariable String methodCode,
            @RequestBody(required = false) PaymentMethodConfigRequest request
    ) {
        paymentsApi.enableMethod(currentStoreHolder.get(), methodCode, configOf(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update payment method config",
            description = "Updates the configuration of a connected payment method")
    @PatchMapping("/{methodCode}")
    public ResponseEntity<Void> updateMethodConfig(
            @PathVariable String methodCode,
            @RequestBody PaymentMethodConfigRequest request
    ) {
        paymentsApi.updateMethodConfig(currentStoreHolder.get(), methodCode, configOf(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Disable payment method",
            description = "Disables a connected payment method; its configuration is preserved")
    @DeleteMapping("/{methodCode}")
    public ResponseEntity<Void> disableMethod(@PathVariable String methodCode) {
        paymentsApi.disableMethod(currentStoreHolder.get(), methodCode);
        return ResponseEntity.noContent().build();
    }

    private Map<String, String> configOf(PaymentMethodConfigRequest request) {
        return request == null || request.config() == null ? Map.of() : request.config();
    }
}

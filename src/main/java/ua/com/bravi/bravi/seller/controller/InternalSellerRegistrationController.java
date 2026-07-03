package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.account.api.SellerRegistrationApi;
import ua.com.bravi.bravi.seller.account.api.SellerRegistrationView;
import ua.com.bravi.bravi.seller.controller.dto.in.SellerRegistrationRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.SellerRegistrationResponse;
import ua.com.bravi.bravi.seller.controller.mapper.SellerRegistrationDtoMapper;

/**
 * Internal, service-to-service endpoint invoked only by the Auth Service (see SecurityConfig:
 * {@code /internal/**} requires the service authority). Not reachable by end users or the public internet.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/registrations")
@Tag(name = "InternalSellerRegistrationController")
public class InternalSellerRegistrationController {

    private final SellerRegistrationApi sellerRegistrationApi;
    private final SellerRegistrationDtoMapper sellerRegistrationDtoMapper;

    @Operation(summary = "Register seller",
            description = "Creates the seller business context (User + Account + SellerAccount + Membership). Idempotent.")
    @PostMapping("/seller")
    public ResponseEntity<SellerRegistrationResponse> registerSeller(
            @Valid @RequestBody SellerRegistrationRequest request) {
        SellerRegistrationView view = sellerRegistrationApi.register(
                sellerRegistrationDtoMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerRegistrationDtoMapper.toResponse(view));
    }
}

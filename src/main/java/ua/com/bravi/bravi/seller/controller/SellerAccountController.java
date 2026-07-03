package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.account.api.SellerAccountView;
import ua.com.bravi.bravi.seller.account.api.SellerAccountsApi;
import ua.com.bravi.bravi.seller.controller.dto.in.SellerAccountCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.SellerAccountResponse;
import ua.com.bravi.bravi.seller.controller.mapper.SellerAccountDtoMapper;
import ua.com.bravi.bravi.shared.component.PermitNoStore;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/accounts")
@PreAuthorize("hasAuthority('role_seller')")
@Tag(name = "SellerAccountController")
public class SellerAccountController {

    private final SellerAccountsApi sellerAccountsApi;
    private final SellerAccountDtoMapper sellerAccountDtoMapper;

    @Operation(summary = "Onboard seller", description = "Provisions a seller account (account + owner membership) for the current user")
    @PostMapping
    @PermitNoStore
    public ResponseEntity<SellerAccountResponse> onboard(@Valid @RequestBody SellerAccountCreateRequest request) {
        SellerAccountView view = sellerAccountsApi.onboardCurrentUser(
                sellerAccountDtoMapper.toRegistration(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerAccountDtoMapper.toResponse(view));
    }
}

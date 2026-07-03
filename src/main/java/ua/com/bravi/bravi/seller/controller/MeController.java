package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.MeService;
import ua.com.bravi.bravi.seller.controller.dto.out.MeResponse;

/**
 * Current-user read surface. Authenticated; the user must already be registered — an unknown
 * identity yields 404 (registration is the only creator). Not under {@code /seller/**}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/me")
@Tag(name = "MeController")
public class MeController {

    private final MeService meService;

    @Operation(summary = "Current user accounts",
            description = "Returns the current user and the accounts they belong to; syncs email_verified from the JWT.")
    @GetMapping("/accounts")
    public MeResponse getAccounts() {
        return meService.currentUserAccounts();
    }
}

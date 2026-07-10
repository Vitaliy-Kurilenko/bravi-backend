package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.AccountService;
import ua.com.bravi.bravi.seller.controller.dto.out.AccountsResponse;

/**
 * Current-user read surface. Authenticated; the user must already be registered — an unknown
 * identity yields 404 (registration is the only creator).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
@Tag(name = "AccountController")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Current user accounts",
            description = "Returns the current user and the accounts they belong to; syncs email_verified from the JWT.")
    @GetMapping
    public AccountsResponse getAccounts() {
        return accountService.currentUserAccounts();
    }
}

package leonardo.banking_transactions.controller;

import jakarta.validation.Valid;
import leonardo.banking_transactions.dto.accounts.AccountCreateRequest;
import leonardo.banking_transactions.dto.accounts.AccountResponse;
import leonardo.banking_transactions.middleware.JwtAuthenticationMiddleware;
import leonardo.banking_transactions.service.AccountsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Accounts", description = "Account operations")
@RequestMapping("/api/accounts/{userId}")
public class AccountsController {
    private final AccountsService accountsService;

    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    @Operation(summary = "Account endpoint", description = "Creates an account for the authenticated user.")
    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @PathVariable UUID userId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId,
            @Valid @RequestBody AccountCreateRequest request) {
        var account = accountsService.create(authenticatedUserId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(account.getId()).toUri();
        return ResponseEntity.created(location).body(AccountResponse.from(account));
    }

    @Operation(summary = "Query accounts", description = "Returns all accounts belonging to the authenticated user.")
    @GetMapping
    public ResponseEntity<List<AccountResponse>> findAll(
            @PathVariable UUID userId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        return ResponseEntity.ok(accountsService.findByUser(authenticatedUserId).stream()
                .map(AccountResponse::from).toList());
    }

    @Operation(summary = "Query accounts", description = "Returns a specific account belonging to the authenticated user.")
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> findById(
            @PathVariable UUID userId,
            @PathVariable UUID accountId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        return ResponseEntity.ok(AccountResponse.from(accountsService.findById(authenticatedUserId, accountId)));
    }

    @Operation(summary = "Delete account", description = "Deletes an account belonging to the authenticated user.")
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID userId,
            @PathVariable UUID accountId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        accountsService.delete(authenticatedUserId, accountId);
        return ResponseEntity.noContent().build();
    }
}

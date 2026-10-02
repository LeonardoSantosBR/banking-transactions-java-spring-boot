package leonardo.banking_transactions.controllers;

import jakarta.validation.Valid;
import leonardo.banking_transactions.dtos.AccountCreateRequest;
import leonardo.banking_transactions.dtos.AccountResponse;
import leonardo.banking_transactions.middlewares.JwtAuthenticationMiddleware;
import leonardo.banking_transactions.services.AccountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/{userId}")
public class AccountsController {
    private final AccountsService accountsService;

    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

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

    @GetMapping
    public ResponseEntity<List<AccountResponse>> findAll(
            @PathVariable UUID userId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        return ResponseEntity.ok(accountsService.findByUser(authenticatedUserId).stream()
                .map(AccountResponse::from).toList());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> findById(
            @PathVariable UUID userId,
            @PathVariable UUID accountId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        return ResponseEntity.ok(AccountResponse.from(accountsService.findById(authenticatedUserId, accountId)));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID userId,
            @PathVariable UUID accountId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        accountsService.delete(authenticatedUserId, accountId);
        return ResponseEntity.noContent().build();
    }
}
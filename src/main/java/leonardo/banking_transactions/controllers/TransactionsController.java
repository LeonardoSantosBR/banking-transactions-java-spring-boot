package leonardo.banking_transactions.controllers;

import jakarta.validation.Valid;
import leonardo.banking_transactions.dtos.*;
import leonardo.banking_transactions.services.TransactionsService;
import leonardo.banking_transactions.middlewares.JwtAuthenticationMiddleware;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/transactions")
public class TransactionsController {
    private final TransactionsService transactionsService;

    public TransactionsController(TransactionsService service) {
        this.transactionsService = service;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionCreateRequest request) {
        var transaction = transactionsService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(transaction.getId()).toUri();
        return ResponseEntity.created(location).body(TransactionResponse.from(transaction));
    }

    @GetMapping
    public List<TransactionResponse> findAll(HttpServletRequest request) {
        return transactionsService.findAll(authenticatedUserId(request)).stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(@PathVariable UUID id, HttpServletRequest request) {
        return TransactionResponse.from(transactionsService.findById(id, authenticatedUserId(request)));
    }

    private UUID authenticatedUserId(HttpServletRequest request) {
        return (UUID) request.getAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE);
    }
}

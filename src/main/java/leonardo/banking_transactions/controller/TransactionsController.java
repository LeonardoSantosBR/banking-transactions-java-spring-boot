package leonardo.banking_transactions.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import leonardo.banking_transactions.dto.transactions.TransactionCreateRequest;
import leonardo.banking_transactions.dto.transactions.TransactionResponse;
import leonardo.banking_transactions.enums.TransactionStatusEnum;
import leonardo.banking_transactions.middleware.JwtAuthenticationMiddleware;
import leonardo.banking_transactions.service.transactions.TransactionsService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "Pix transactions", description = "Pix transaction operations")
@RequestMapping("/api/transactions")
public class TransactionsController {
    private final TransactionsService transactionsService;

    public TransactionsController(TransactionsService service) {
        this.transactionsService = service;
    }

    @Operation(summary = "Pix transaction endpoint", description = "Creates a Pix transaction for the authenticated user.")
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody TransactionCreateRequest requestBody) {
        UUID authenticatedUserId = (UUID) request
                .getAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE);
        var transaction = transactionsService.create(authenticatedUserId, requestBody);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(transaction.getId()).toUri();
        return ResponseEntity.created(location).body(TransactionResponse.from(transaction));
    }

    @Operation(summary = "Pix transaction endpoint", description = "Refunds an eligible Pix transaction and returns the created refund transaction.")
    @PostMapping("/{id}/refund")
    public ResponseEntity<TransactionResponse> refund(@PathVariable UUID id, HttpServletRequest request) {
        var refund = transactionsService.refund(id, authenticatedUserId(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{refundId}")
                .buildAndExpand(refund.getId()).toUri();
        return ResponseEntity.created(location).body(TransactionResponse.from(refund));
    }

    @Operation(summary = "Query Pix transactions", description = "Returns a specific Pix transaction belonging to the authenticated user.")
    @GetMapping
    public Page<TransactionResponse> findAll(
            @RequestParam(required = false) TransactionStatusEnum status,
            @RequestParam(required = false) OffsetDateTime createdFrom,
            @RequestParam(required = false) OffsetDateTime createdTo,
            @RequestParam(required = false) BigDecimal amountFrom,
            @RequestParam(required = false) BigDecimal amountTo,
            @RequestParam(required = false) String txid,
            @RequestParam(required = false) String endToEndId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest request) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return transactionsService.findAll(authenticatedUserId(request), status, createdFrom, createdTo,
                amountFrom, amountTo, txid, endToEndId, pageable).map(TransactionResponse::from);
    }

    @Operation(summary = "Query Pix transactions", description = "Returns a paginated list of the authenticated user's Pix transactions with optional filters.")
    @GetMapping("/{id}")
    public TransactionResponse findById(@PathVariable UUID id, HttpServletRequest request) {
        return TransactionResponse.from(transactionsService.findById(id, authenticatedUserId(request)));
    }

    private UUID authenticatedUserId(HttpServletRequest request) {
        return (UUID) request.getAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE);
    }
}

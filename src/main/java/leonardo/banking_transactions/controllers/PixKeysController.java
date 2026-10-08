package leonardo.banking_transactions.controllers;

import jakarta.validation.Valid;
import leonardo.banking_transactions.dtos.pixKeys.PixKeyCreateRequest;
import leonardo.banking_transactions.dtos.pixKeys.PixKeyResponse;
import leonardo.banking_transactions.dtos.pixKeys.PixKeyUpdateRequest;
import leonardo.banking_transactions.middlewares.JwtAuthenticationMiddleware;
import leonardo.banking_transactions.services.PixKeysService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pix-keys/{accountId}")
public class PixKeysController {
    private final PixKeysService pixKeysService;

    public PixKeysController(PixKeysService pixKeysService) {
        this.pixKeysService = pixKeysService;
    }

    @PostMapping
    public ResponseEntity<PixKeyResponse> create(
            @PathVariable UUID accountId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId,
            @Valid @RequestBody PixKeyCreateRequest request) {
        var key = pixKeysService.create(authenticatedUserId, accountId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(key.getId()).toUri();
        return ResponseEntity.created(location).body(PixKeyResponse.from(key));
    }

    @GetMapping
    public List<PixKeyResponse> findAll(
            @PathVariable UUID accountId,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        return pixKeysService.findByAccount(authenticatedUserId, accountId).stream()
                .map(PixKeyResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PixKeyResponse findById(
            @PathVariable UUID accountId,
            @PathVariable UUID id,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        return PixKeyResponse.from(pixKeysService.findById(authenticatedUserId, accountId, id));
    }

    @PutMapping("/{id}")
    public PixKeyResponse update(
            @PathVariable UUID accountId,
            @PathVariable UUID id,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId,
            @Valid @RequestBody PixKeyUpdateRequest request) {
        return PixKeyResponse.from(pixKeysService.update(authenticatedUserId, accountId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID id,
            @RequestAttribute(JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE) UUID authenticatedUserId) {
        pixKeysService.delete(authenticatedUserId, accountId, id);
        return ResponseEntity.noContent().build();
    }
}
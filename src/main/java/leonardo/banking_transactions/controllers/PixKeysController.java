package leonardo.banking_transactions.controllers;

import jakarta.validation.Valid;
import leonardo.banking_transactions.dtos.*;
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

    private final PixKeysService service;

    public PixKeysController(PixKeysService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PixKeyResponse> create(@PathVariable UUID accountId,
            @Valid @RequestBody PixKeyCreateRequest request) {
        var key = service.create(accountId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(key.getId())
                .toUri();
        return ResponseEntity.created(location).body(PixKeyResponse.from(key));
    }

    @GetMapping
    public List<PixKeyResponse> findAll(@PathVariable UUID accountId) {
        return service.findByAccount(accountId).stream().map(PixKeyResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PixKeyResponse findById(@PathVariable UUID accountId, @PathVariable UUID id) {
        return PixKeyResponse.from(service.findById(accountId, id));
    }

    @PutMapping("/{id}")
    public PixKeyResponse update(@PathVariable UUID accountId, @PathVariable UUID id,
            @Valid @RequestBody PixKeyUpdateRequest request) {
        return PixKeyResponse.from(service.update(accountId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID accountId, @PathVariable UUID id) {
        service.delete(accountId, id);
        return ResponseEntity.noContent().build();
    }
}

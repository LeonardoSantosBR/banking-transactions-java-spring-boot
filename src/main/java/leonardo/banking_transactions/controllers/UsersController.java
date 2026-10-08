package leonardo.banking_transactions.controllers;

import leonardo.banking_transactions.entities.UsersEntity;
import leonardo.banking_transactions.dtos.users.UserCreateRequest;
import leonardo.banking_transactions.dtos.users.UserResponse;
import leonardo.banking_transactions.dtos.users.UserSummaryResponse;
import leonardo.banking_transactions.dtos.users.UserUpdateRequest;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import leonardo.banking_transactions.middlewares.JwtAuthenticationMiddleware;
import leonardo.banking_transactions.services.UsersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UsersEntity createdUser = usersService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(createdUser));
    }

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> findAll() {
        return ResponseEntity.ok(usersService.findAllActive().stream().map(UserSummaryResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(usersService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(UserResponse.from(usersService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        UUID authenticatedUserId = (UUID) request.getAttribute(
                JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE);
        usersService.delete(authenticatedUserId, id);
        return ResponseEntity.noContent().build();
    }
}

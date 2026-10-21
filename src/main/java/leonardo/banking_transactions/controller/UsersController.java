package leonardo.banking_transactions.controller;

import leonardo.banking_transactions.dto.users.UserCreateRequest;
import leonardo.banking_transactions.dto.users.UserResponse;
import leonardo.banking_transactions.dto.users.UserSummaryResponse;
import leonardo.banking_transactions.dto.users.UserUpdateRequest;
import leonardo.banking_transactions.entity.UsersEntity;
import leonardo.banking_transactions.middleware.JwtAuthenticationMiddleware;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import leonardo.banking_transactions.service.UsersService;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Users", description = "User operations")
@RequestMapping("/api/users")
public class UsersController {
    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @Operation(summary = "User endpoint")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UsersEntity createdUser = usersService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(createdUser));
    }

    @Operation(summary = "Query users")
    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> findAll() {
        return ResponseEntity.ok(usersService.findAllActive().stream().map(UserSummaryResponse::from).toList());
    }

    @Operation(summary = "Query users")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(usersService.findById(id)));
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(UserResponse.from(usersService.update(id, request)));
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        UUID authenticatedUserId = (UUID) request.getAttribute(
                JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE);
        usersService.delete(authenticatedUserId, id);
        return ResponseEntity.noContent().build();
    }
}

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@Tag(name = "Users", description = "User operations")
@RequestMapping("/api/users")
public class UsersController {
    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @Operation(summary = "User endpoint", description = "Creates a new user account.")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<UserResponse> create(
            @RequestParam @NotBlank @Size(max = 150) String name,
            @RequestParam @NotBlank @Size(max = 11) String cpf,
            @RequestParam @NotBlank @Email @Size(max = 150) String email,
            @RequestParam(required = false) @Size(max = 20) String phone,
            @RequestParam @NotBlank @Size(min = 6, max = 100) String password,
            @RequestPart(value = "avatar_url", required = false) MultipartFile avatar) {
        UserCreateRequest request = new UserCreateRequest(name, cpf, email, phone, password);
        UsersEntity createdUser = usersService.create(request, avatar);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(createdUser));
    }

    @Operation(summary = "Query users", description = "Returns all active users in summary format.")
    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> findAll() {
        return ResponseEntity.ok(usersService.findAllActive().stream().map(UserSummaryResponse::from).toList());
    }

    @Operation(summary = "Query users", description = "Returns a user by identifier.")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(usersService.findById(id)));
    }

    @Operation(summary = "Update user", description = "Updates the profile data of a user.")
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @RequestParam @NotBlank @Size(max = 150) String name,
            @RequestParam @NotBlank @Size(max = 11) String cpf,
            @RequestParam @NotBlank @Email @Size(max = 150) String email,
            @RequestParam(required = false) @Size(max = 20) String phone,
            @RequestParam(required = false) @Size(min = 6, max = 100) String password,
            @RequestPart(value = "avatar_url", required = false) MultipartFile avatar) {
        UserUpdateRequest request = new UserUpdateRequest(name, cpf, email, phone, password);
        return ResponseEntity.ok(UserResponse.from(usersService.update(id, request, avatar)));
    }

    @Operation(summary = "Delete user", description = "Deletes a user when the authenticated user is authorized.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        UUID authenticatedUserId = (UUID) request.getAttribute(
                JwtAuthenticationMiddleware.AUTHENTICATED_USER_ID_ATTRIBUTE);
        usersService.delete(authenticatedUserId, id);
        return ResponseEntity.noContent().build();
    }
}

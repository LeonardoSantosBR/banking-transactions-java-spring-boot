package leonardo.banking_transactions.controller;

import jakarta.validation.Valid;
import leonardo.banking_transactions.dto.auth.RefreshTokenRequest;
import leonardo.banking_transactions.dto.auth.SigninRequest;
import leonardo.banking_transactions.dto.auth.SigninResponse;
import leonardo.banking_transactions.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@Tag(name = "Authentication", description = "Authentication operations")
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Authentication endpoint")
    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(@Valid @RequestBody SigninRequest request) {
        return ResponseEntity.ok(authService.signin(request));
    }

    @Operation(summary = "Authentication endpoint")
    @PostMapping("/refresh")
    public ResponseEntity<SigninResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
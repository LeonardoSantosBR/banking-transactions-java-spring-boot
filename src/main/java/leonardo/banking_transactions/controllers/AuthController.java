package leonardo.banking_transactions.controllers;

import jakarta.validation.Valid;
import leonardo.banking_transactions.dtos.signin.SigninRequest;
import leonardo.banking_transactions.dtos.signin.SigninResponse;
import leonardo.banking_transactions.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(@Valid @RequestBody SigninRequest request) {
        return ResponseEntity.ok(authService.signin(request));
    }
}

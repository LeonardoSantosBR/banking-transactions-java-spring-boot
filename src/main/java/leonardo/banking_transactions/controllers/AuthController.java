package leonardo.banking_transactions.controllers;

import jakarta.validation.Valid;
import leonardo.banking_transactions.dtos.LoginRequest;
import leonardo.banking_transactions.dtos.LoginResponse;
import leonardo.banking_transactions.services.UsersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsersService usersService;

    public AuthController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usersService.login(request));
    }
}

package leonardo.pix_simulation.controllers;

import jakarta.validation.Valid;
import leonardo.pix_simulation.dtos.LoginRequest;
import leonardo.pix_simulation.dtos.LoginResponse;
import leonardo.pix_simulation.services.UsersService;
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

package leonardo.pix_simulation.controllers;

import leonardo.pix_simulation.entities.UsersEntity;
import leonardo.pix_simulation.services.UsersService;
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
    public ResponseEntity<UsersEntity> create(@RequestBody UsersEntity user) {
        UsersEntity createdUser = usersService.create(user);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdUser);
    }

    @GetMapping
    public ResponseEntity<List<UsersEntity>> findAll() {
        return ResponseEntity.ok(usersService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsersEntity> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(usersService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsersEntity> update(
            @PathVariable UUID id,
            @RequestBody UsersEntity user
    ) {
        return ResponseEntity.ok(usersService.update(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        usersService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

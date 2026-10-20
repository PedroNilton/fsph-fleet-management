package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.api.v1.dtos.requests.CreateUserRequest;
import br.com.gestaofrota.api.v1.dtos.requests.UpdateUserRequest;
import br.com.gestaofrota.api.v1.dtos.responses.UserResponse;
import br.com.gestaofrota.application.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USERS_READ')")
    public List<UserResponse> list() {
        return userService.list().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_READ')")
    public UserResponse find(@PathVariable UUID id) {
        return UserResponse.from(userService.find(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USERS_WRITE')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = UserResponse.from(userService.create(
                request.sectorId(),
                request.name(),
                request.email(),
                request.password(),
                request.role()
        ));

        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_WRITE')")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.update(
                id,
                request.sectorId(),
                request.name(),
                request.email(),
                request.password(),
                request.role(),
                request.active()
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_WRITE')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

package br.com.gestaofrota.api.v1.controllers;

import br.com.gestaofrota.application.services.AuthService;
import br.com.gestaofrota.application.results.LoginResult;
import br.com.gestaofrota.api.v1.dtos.requests.LoginRequest;
import br.com.gestaofrota.api.v1.dtos.responses.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request.email(), request.password());
        LoginResponse response = new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresIn()
        );

        return ResponseEntity.ok(response);
    }
}

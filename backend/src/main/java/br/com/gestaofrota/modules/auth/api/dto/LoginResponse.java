package br.com.gestaofrota.modules.auth.api.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
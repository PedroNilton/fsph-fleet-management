package br.com.gestaofrota.api.v1.dtos.responses;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

package br.com.gestaofrota.application.results;

public record LoginResult(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

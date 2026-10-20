package br.com.gestaofrota.api.v1.dtos;

public record LoginResult(String accessToken, long expiresIn) {
}

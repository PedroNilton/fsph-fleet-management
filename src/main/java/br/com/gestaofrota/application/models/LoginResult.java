package br.com.gestaofrota.application.models;

public record LoginResult(String accessToken, long expiresIn) {
}

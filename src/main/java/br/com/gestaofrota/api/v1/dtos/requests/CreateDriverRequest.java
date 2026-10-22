package br.com.gestaofrota.api.v1.dtos.requests;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDriverRequest(@NotNull UUID userId) {
}
package br.com.gestaofrota.api.v1.dtos.requests;

import br.com.gestaofrota.domain.enums.Permission;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ReplacePermissionsRequest(@NotNull Set<@NotNull Permission> permissions) {
}

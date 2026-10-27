package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.Permission;
import br.com.gestaofrota.domain.enums.UserRole;

import java.util.Set;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        UUID sectorId,
        String name,
        String email,
        UserRole role,
        Set<Permission> permissions
) {
}

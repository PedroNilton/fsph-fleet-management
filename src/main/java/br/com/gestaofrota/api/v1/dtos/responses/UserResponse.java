package br.com.gestaofrota.api.v1.dtos.responses;

import br.com.gestaofrota.domain.enums.UserRole;
import br.com.gestaofrota.domain.models.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID sectorId,
        String sectorName,
        String name,
        String email,
        UserRole role,
        boolean active
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getSector().getId(),
                user.getSector().getName(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }
}

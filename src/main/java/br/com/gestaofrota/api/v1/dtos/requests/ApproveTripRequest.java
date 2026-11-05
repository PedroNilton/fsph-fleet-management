package br.com.gestaofrota.api.v1.dtos.requests;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ApproveTripRequest(
        UUID vehicleId,

        @Size(max = 500, message = "As observações da decisão devem ter no máximo 500 caracteres")
        String decisionNotes
) {
}

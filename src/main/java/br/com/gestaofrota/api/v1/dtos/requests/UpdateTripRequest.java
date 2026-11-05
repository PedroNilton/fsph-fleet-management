package br.com.gestaofrota.api.v1.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateTripRequest(
        @NotNull(message = "A data/hora de saída é obrigatória")
        Instant departureAt,

        @NotNull(message = "A data/hora prevista de retorno é obrigatória")
        Instant expectedReturnAt,

        @NotBlank(message = "A origem é obrigatória")
        @Size(max = 200, message = "A origem deve ter no máximo 200 caracteres")
        String origin,

        @NotBlank(message = "O destino é obrigatório")
        @Size(max = 200, message = "O destino deve ter no máximo 200 caracteres")
        String destination,

        @NotBlank(message = "A finalidade é obrigatória")
        @Size(max = 500, message = "A finalidade deve ter no máximo 500 caracteres")
        String purpose,

        @NotNull(message = "O número de passageiros é obrigatório")
        @Min(value = 1, message = "O número de passageiros deve ser pelo menos 1")
        Integer passengerCount,

        UUID requestedVehicleId
) {
}

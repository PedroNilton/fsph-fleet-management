package br.com.gestaofrota.api.v1.dtos.requests;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ScheduleTripRequest(
        @NotNull(message = "O veículo é obrigatório para o agendamento")
        UUID vehicleId,

        @NotNull(message = "O motorista é obrigatório para o agendamento")
        UUID driverId
) {
}

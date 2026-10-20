package br.com.gestaofrota.domain.enums;

public enum VehicleStatus {
    GARAGEM, // veículo disponível ou estacionado na base.
    TRAJETO, //veículo em deslocamento.
    RETORNO, //veículo retornando à base.
    MANUTENCAO //veículo indisponível para uso por estar em manutenção.
}

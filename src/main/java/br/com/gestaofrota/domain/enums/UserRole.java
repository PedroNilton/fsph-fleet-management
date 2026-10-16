package br.com.gestaofrota.domain.enums;

public enum UserRole {
    ADMIN,
    EMPLOYEE, // solicita viagem, acompanha status, informa origem, destino...
    GESTOR_FROTA, // aprova solicitações, agenda viagens, gera relatórios, acompanha indicadores e visualiza frota
    MOTORISTA, // visualiza viagens dele, consulta veículos, registra inicio e termino e registra abastecimento
    ATENDENTE_SVO,
    GESTOR_SVO,
    EQUIPE_COLETA_SVO
}

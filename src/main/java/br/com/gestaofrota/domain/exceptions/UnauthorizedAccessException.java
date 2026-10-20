package br.com.gestaofrota.domain.exceptions;

public class UnauthorizedAccessException extends ExceptionMessageBuilder {

    public UnauthorizedAccessException() {
        super(
                "Acesso negado",
                "Você não possui permissão para executar esta operação.",
                403,
                "ACCESS_DENIED"
        );
    }
}

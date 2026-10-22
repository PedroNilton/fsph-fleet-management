package br.com.gestaofrota.domain.exceptions;

public class InvalidRequestException extends ExceptionMessageBuilder {
    public InvalidRequestException(String message) {
        super("Erro de validação", message, 400, "VALIDATION_ERROR");
    }
}
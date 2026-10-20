package br.com.gestaofrota.domain.exceptions;

public class ConflictException extends ExceptionMessageBuilder {

    public ConflictException(String message) {
        super("Conflito de dados", message, 409, "DATA_CONFLICT");
    }
}

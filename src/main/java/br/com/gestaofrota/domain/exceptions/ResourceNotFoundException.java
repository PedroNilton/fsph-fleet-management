package br.com.gestaofrota.domain.exceptions;

public class ResourceNotFoundException extends ExceptionMessageBuilder {

    public ResourceNotFoundException(String resource, Object id) {
        super(
                "Resource not found",
                "%s with identifier '%s' was not found.".formatted(resource, id),
                404
        );
    }
}
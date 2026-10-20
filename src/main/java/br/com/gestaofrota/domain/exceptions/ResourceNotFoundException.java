package br.com.gestaofrota.domain.exceptions;

public class ResourceNotFoundException extends ExceptionMessageBuilder {

    public ResourceNotFoundException(String resource, Object id) {
        super(
                "Recurso não encontrado",
                "%s com identificador '%s' não encontrado.".formatted(resource, id),
                404,
                "RESOURCE_NOT_FOUND"
        );
    }
}

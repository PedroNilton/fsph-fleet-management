package br.com.gestaofrota.domain.exceptions;

public class UnauthorizedAccessException extends ExceptionMessageBuilder {

    public UnauthorizedAccessException() {
        super(
                "Unauthorized Access",
                "You are not authorized to access this resource.",
                403
        );
    }
}

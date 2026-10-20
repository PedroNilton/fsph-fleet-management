package br.com.gestaofrota.api.v1.errors;

import br.com.gestaofrota.domain.exceptions.InvalidTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    ProblemDetail handleInvalidToken(InvalidTokenException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Token inválido ou expirado");
        problem.setTitle("Falha na autenticação");
        problem.setProperty("code", "INVALID_TOKEN");
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthenticationException(AuthenticationException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Falha na autenticação");
        problem.setDetail("E-mail ou senha inválidos");
        problem.setProperty("code", "AUTHENTICATION_FAILED");
        return problem;
    }
}

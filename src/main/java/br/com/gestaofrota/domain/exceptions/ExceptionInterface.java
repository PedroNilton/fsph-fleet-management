package br.com.gestaofrota.domain.exceptions;

public interface ExceptionInterface {
    String getTitle();
    String getMessage();
    int getStatusCode();
}

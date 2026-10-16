package br.com.gestaofrota.domain.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class ExceptionMessageBuilder extends RuntimeException implements ExceptionInterface {
    private final String title;
    private final String message;
    private final int statusCode;
}

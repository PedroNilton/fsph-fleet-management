package br.com.gestaofrota.domain.exceptions;

public abstract class ExceptionMessageBuilder extends RuntimeException implements ExceptionInterface {
    private final String title;
    private final int statusCode;
    private final String code;

    protected ExceptionMessageBuilder(String title, String message, int statusCode, String code) {
        super(message);
        this.title = title;
        this.statusCode = statusCode;
        this.code = code;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getCode() {
        return code;
    }
}

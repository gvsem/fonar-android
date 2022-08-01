package ru.georgii.fonar.core.exception;

public class NotASupportedFonarServerException extends FonarException {
    public NotASupportedFonarServerException() {
        super();
    }

    public NotASupportedFonarServerException(String message) {
        super(message);
    }

    public NotASupportedFonarServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotASupportedFonarServerException(Throwable cause) {
        super(cause);
    }
}

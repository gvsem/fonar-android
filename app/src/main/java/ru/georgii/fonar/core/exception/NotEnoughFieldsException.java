package ru.georgii.fonar.core.exception;

public class NotEnoughFieldsException extends FonarException {
    public NotEnoughFieldsException() {
        super();
    }

    public NotEnoughFieldsException(String message) {
        super(message);
    }

    public NotEnoughFieldsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughFieldsException(Throwable cause) {
        super(cause);
    }
}

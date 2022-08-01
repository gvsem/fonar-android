package ru.georgii.fonar.core.exception;

public class MalformedSocketMessageException extends FonarException {
    public MalformedSocketMessageException() {
        super();
    }

    public MalformedSocketMessageException(String message) {
        super(message);
    }

    public MalformedSocketMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedSocketMessageException(Throwable cause) {
        super(cause);
    }
}

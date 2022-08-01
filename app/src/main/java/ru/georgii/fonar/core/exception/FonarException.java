package ru.georgii.fonar.core.exception;

public class FonarException extends Exception {

    public FonarException() {
        super();
    }

    public FonarException(String message) {
        super(message);
    }

    public FonarException(String message, Throwable cause) {
        super(message, cause);
    }

    public FonarException(Throwable cause) {
        super(cause);
    }

}

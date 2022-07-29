package ru.georgii.fonar.core.exception;

public class FonarServerException extends Exception {

    public FonarServerException() {
        super();
    }

    public FonarServerException(String message) {
        super(message);
    }

    public FonarServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public FonarServerException(Throwable cause) {
        super(cause);
    }

}

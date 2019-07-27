package httpService.exceptions;

public class BadResponseException extends RuntimeException {
    public BadResponseException() {
        super();
    }

    public BadResponseException(String message) {
        super(message);
    }

    public BadResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadResponseException(Throwable cause) {
        super(cause);
    }
}

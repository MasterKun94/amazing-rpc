package httpService.exceptions;

public class UnhandledException extends RuntimeException {
    public UnhandledException() {
        super();
    }

    public UnhandledException(String message) {
        super(message);
    }

    public UnhandledException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnhandledException(Throwable cause) {
        super(cause);
    }
}

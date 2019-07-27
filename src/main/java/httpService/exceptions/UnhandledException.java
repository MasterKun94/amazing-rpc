package httpService.exceptions;

public class UnhandledException extends RuntimeException {

    private static final String message = "If you are seeing this message, it usually " +
            "means you didn't add a FallBackMethod for this ResponseFuture or your " +
            "FallBackMethod didn't handle this Exception, so the Exception is thrown " +
            "in the end, and the exception type is: ";

    public UnhandledException(Throwable cause, CauseType causeType) {

        super(message + causeType, cause);//TODO
    }
}

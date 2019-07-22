package httpService.exceptions;

public class ConnectionException extends RpcServiceException {

    public ConnectionException(CauseType type) {
        super(type);
    }

    public ConnectionException(CauseType type, String message) {
        super(type, message);
    }

    public ConnectionException(CauseType type, String message, Throwable cause) {
        super(type, message, cause);
    }

    public ConnectionException(CauseType type, Throwable cause) {
        super(type, cause);
    }

    public static class Failed extends ConnectionException {

        public Failed() {
            super(CauseType.CONNECTION_CONNECT_FAILED);
        }

        public Failed(String message) {
            super(CauseType.CONNECTION_CONNECT_FAILED, message);
        }

        public Failed(String message, Throwable cause) {
            super(CauseType.CONNECTION_CONNECT_FAILED, message, cause);
        }

        public Failed(Throwable cause) {
            super(CauseType.CONNECTION_CONNECT_FAILED, cause);
        }
    }

    public static class ConnectTimeout extends ConnectionException {

        public ConnectTimeout() {
            super(CauseType.CONNECTION_CONNECT_TIMEOUT);
        }

        public ConnectTimeout(String message) {
            super(CauseType.CONNECTION_CONNECT_TIMEOUT, message);
        }

        public ConnectTimeout(String message, Throwable cause) {
            super(CauseType.CONNECTION_CONNECT_TIMEOUT, message, cause);
        }

        public ConnectTimeout(Throwable cause) {
            super(CauseType.CONNECTION_CONNECT_TIMEOUT, cause);
        }
    }

    public static class RequestTimeout extends ConnectionException {

        public RequestTimeout() {
            super(CauseType.CONNECTION_REQUEST_TIMEOUT);
        }

        public RequestTimeout(String message) {
            super(CauseType.CONNECTION_REQUEST_TIMEOUT, message);
        }

        public RequestTimeout(String message, Throwable cause) {
            super(CauseType.CONNECTION_REQUEST_TIMEOUT, message, cause);
        }

        public RequestTimeout(Throwable cause) {
            super(CauseType.CONNECTION_REQUEST_TIMEOUT, cause);
        }
    }
}

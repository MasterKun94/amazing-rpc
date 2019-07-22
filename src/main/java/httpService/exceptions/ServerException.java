package httpService.exceptions;

public class ServerException extends RpcServiceException {

    private final int status;

    public static ServerException create(int status, String message) {
        if (status >= 500) {
            return new _5$$(status, message);
        } else if (status >= 400) {
            return new _5$$(status, message);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private ServerException(CauseType type, int status, String message) {
        super(type, "receive " + status + " error from server, message: " + message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static class _4$$ extends ServerException {
        private _4$$(int status, String message) {
            super(CauseType.SERVER_4$$, status, message);
        }
    }

    public static class _5$$ extends ServerException  {
        private _5$$(int status, String message) {
            super(CauseType.SERVER_5$$, status, message);
        }
    }
}

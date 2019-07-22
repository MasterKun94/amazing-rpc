package httpService.exceptions;

public class RpcServiceException extends Exception {

    private CauseType type;

    public RpcServiceException(CauseType type) {
        super();
        this.type = type;
    }

    public RpcServiceException(CauseType type, String message) {
        super(message);
        this.type = type;
    }

    public RpcServiceException(CauseType type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public RpcServiceException(CauseType type, Throwable cause) {
        super(cause);
        this.type = type;
    }

    public CauseType getType() {
        return type;
    }
}

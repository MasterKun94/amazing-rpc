package httpService.connectors.netty;

import httpService.exceptions.CauseType;

public interface ResponsePromise<T> extends ResponseFuture<T> {
    boolean receive(T entity);

    boolean receive(Throwable cause, CauseType type);

    boolean receive(T entity, Throwable cause, CauseType type);

    void setEntity(T entity);

    void setCause(Throwable cause);

    void setCauseType(CauseType type);

    boolean setSuccess(boolean isSuccess);

    static <T> ResponsePromise<T> create() {
        return new ClientResponsePromise<>();
    }
}

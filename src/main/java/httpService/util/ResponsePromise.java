package httpService.util;

import httpService.exceptions.CauseType;

public interface ResponsePromise<T> extends ResponseFuture<T> {
    boolean receive(T entity);

    boolean receive(Throwable cause, CauseType type);

    boolean receive(T entity, Throwable cause, CauseType type);

    static <T> ResponsePromise<T> create() {
        return new ClientResponsePromise<>();
    }

    void setSuccess(boolean isSuccess);

    void setEntity(T entity);

    void setCause(Throwable cause);

    void setCauseType(CauseType causeType);
}

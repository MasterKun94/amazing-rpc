package httpService.connectors.netty;

import httpService.exceptions.CauseType;

public interface ResponsePromise<T> extends ResponseFuture<T> {
    void receive(T entity);

    void receive(Throwable cause, CauseType type);

    void receive(T entity, Throwable cause, CauseType type);

    static <T> ResponsePromise<T> create(Class<T> clazz) {
        return new ClientResponsePromise<>();
    }
}

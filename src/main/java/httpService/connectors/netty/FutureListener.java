package httpService.connectors.netty;

public interface FutureListener<T> {
    void listen(ResponsePromise<T> promise);
}

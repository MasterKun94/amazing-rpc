package httpService.connectors.netty;

public interface FutureListener<T> {
    void listen(ResponseFuture<T> future);
}

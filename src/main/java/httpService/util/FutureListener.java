package httpService.util;

public interface FutureListener<T> {
    void listen(ResponseFuture<T> promise);
}

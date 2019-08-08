package httpService.util;

public interface FutureListener<T> {
    void listen(ResponsePromise<T> promise);
}

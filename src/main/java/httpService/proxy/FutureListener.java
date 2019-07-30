package httpService.proxy;

public interface FutureListener<T> {
    void listen(ResponsePromise<T> promise);
}

package httpService.proxy;

public interface ArgsToPromise<T> {
    ResponseFuture<T> apply(Object[] args);
}

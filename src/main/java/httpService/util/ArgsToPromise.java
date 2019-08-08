package httpService.util;

public interface ArgsToPromise<T> {
    ResponseFuture<T> apply(Object[] args);
}

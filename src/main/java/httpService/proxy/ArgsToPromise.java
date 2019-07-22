package httpService.proxy;

import httpService.connectors.netty.ResponsePromise;

public interface ArgsToPromise<T> {
    ResponsePromise<T> apply(Object[] args);
}

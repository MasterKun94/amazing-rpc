package httpService.connectors;

import httpService.RequestArgs;
import httpService.connectors.netty.ResponsePromise;
import httpService.proxy.ResponseDecoder;

public interface Connector {
    @Deprecated
    <T> T execute(RequestArgs requestArgs, ResponsePromise<T> promise, ResponseDecoder<T> decoder) throws Throwable;

    <T> ResponsePromise<T> executeAsync(RequestArgs requestArgs, ResponsePromise<T> promise, ResponseDecoder<T> decoder);
}

package httpService.connectors;

import httpService.RequestArgs;
import httpService.connectors.netty.ResponsePromise;
import httpService.proxy.ResponseDecoder;

public interface Connector {
    @Deprecated
    <T> T execute(RequestArgs requestArgs, ResponseDecoder<T> decoder, ResponsePromise<T> promise) throws Throwable;

    <T> ResponsePromise<T> executeAsync(RequestArgs requestArgs, ResponseDecoder<T> decoder, ResponsePromise<T> promise);
}

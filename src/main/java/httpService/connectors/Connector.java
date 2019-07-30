package httpService.connectors;

import httpService.proxy.RequestArgs;
import httpService.proxy.Decoder;
import httpService.proxy.ResponseFuture;
import httpService.proxy.ResponsePromise;

public interface Connector {

    <T> ResponseFuture<T> executeAsync(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise);
}

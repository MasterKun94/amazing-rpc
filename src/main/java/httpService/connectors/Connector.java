package httpService.connectors;

import httpService.util.RequestArgs;
import httpService.util.Decoder;
import httpService.util.ResponseFuture;
import httpService.util.ResponsePromise;

public interface Connector {

    <T> ResponseFuture<T> executeAsync(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise);
}

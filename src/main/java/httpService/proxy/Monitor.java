package httpService.proxy;

import httpService.RequestArgs;

public interface Monitor {
    void before(RequestArgs requestArgs, Decoder decoder, ResponsePromise promise);

    void after(ResponseFuture future);
}

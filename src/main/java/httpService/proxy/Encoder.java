package httpService.proxy;

import httpService.RequestArgs;

public interface Encoder {
    RequestArgs encode(Object[] args, ResponsePromise promise);
}

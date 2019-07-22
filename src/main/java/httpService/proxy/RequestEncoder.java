package httpService.proxy;

import httpService.RequestArgs;
import httpService.connectors.netty.ResponsePromise;

public interface RequestEncoder {
    RequestArgs encode(Object[] args, ResponsePromise promise);
}

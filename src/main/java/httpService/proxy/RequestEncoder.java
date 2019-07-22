package httpService.proxy;

import httpService.RequestArgs;

public interface RequestEncoder {
    RequestArgs encode(Object[] args);
}

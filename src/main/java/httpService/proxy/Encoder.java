package httpService.proxy;

public interface Encoder {
    RequestArgs encode(Object[] args, ResponsePromise promise);
}

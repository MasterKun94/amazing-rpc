package httpService.util;

public interface Encoder {
    RequestArgs encode(Object[] args, ResponsePromise promise);
}

package httpService.util;

public interface Decorator {
    void beforeSendRequest();

    void afterReceiveResponse(ResponseFuture future);
}

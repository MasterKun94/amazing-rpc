package httpService.proxy;

public interface Monitor {
    void beforeSendRequest();

    void afterReceiveResponse(ResponseFuture future);
}

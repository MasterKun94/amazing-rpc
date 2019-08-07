package httpService.proxy;

public interface ReleaseAble {
    void release();

    ResponseFuture<Void> close();
}

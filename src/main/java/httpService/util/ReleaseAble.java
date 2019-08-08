package httpService.util;

public interface ReleaseAble {
    void release();

    ResponseFuture<Void> close();
}

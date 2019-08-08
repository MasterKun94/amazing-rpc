package httpService.util;

public interface ReleaseAble {
    void release();

    int getIndex();

    ResponseFuture<Void> close();
}

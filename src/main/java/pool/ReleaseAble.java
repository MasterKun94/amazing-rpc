package pool;

import httpService.proxy.ResponseFuture;

public interface ReleaseAble {
    void release();

    int getIndex();

    ResponseFuture<Void> close();
}

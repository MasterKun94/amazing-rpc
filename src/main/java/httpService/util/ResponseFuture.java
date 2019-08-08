package httpService.util;

import httpService.exceptions.CauseType;

import java.util.concurrent.Future;

public interface ResponseFuture<T> extends Future<T> {

    boolean isDoneAndSuccess();

    boolean isDoneAndFailed();

    boolean whenSuccess(long timeout);

    CauseType getCauseType();

    Throwable getCause();

    T getEntity();

    ResponseFuture<T> addListener(FutureListener<T> listener);
}

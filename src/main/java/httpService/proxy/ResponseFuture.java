package httpService.proxy;

import httpService.exceptions.CauseType;

import java.util.concurrent.Future;

public interface ResponseFuture<T> extends Future<T> {

    boolean isDoneAndSuccess();

    boolean isDoneAndFailed();

    boolean whenSuccess(long timeout);

    CauseType getCauseType();

    Throwable getCause();

    Throwable getCauseAndReset();

    T getEntity();

    T getEntityAndReset();

    ResponseFuture<T> addListener(FutureListener<T> listener);

    boolean reset();
}
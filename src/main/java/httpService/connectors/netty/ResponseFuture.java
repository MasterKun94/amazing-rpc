package httpService.connectors.netty;

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

    ResponseFuture<T> addFallBackMethod(FallBackMethod<T> fallBackMethod);

    boolean reset();
}

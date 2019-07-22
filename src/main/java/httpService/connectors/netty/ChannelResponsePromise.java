package httpService.connectors.netty;

import httpService.exceptions.ConnectionException;
import httpService.exceptions.CauseType;
import httpService.exceptions.UnexpectedException;
import httpService.proxy.FallBackMethod;
import httpService.proxy.ResponseDecoder;
import pool.ChannelHolder;
import pool.ChannelManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

public class ChannelResponsePromise extends AbstractResponsePromise<String> {

    private final ChannelHolder holder;

    public ChannelResponsePromise(ChannelHolder holder) {
        this.holder = holder;
    }

    @Override
    public void receive(String entity) {
        super.receive(entity);
    }

    @Override
    public void receive(Throwable cause, CauseType type) {
        super.receive(cause, type);
    }

    @Override
    public void receive(String entity, Throwable cause, CauseType type) {
        super.receive(entity, cause, type);
    }

    @Override
    public boolean isDoneAndSuccess() {
        return super.isDoneAndSuccess();
    }

    @Override
    public boolean isDoneAndFailed() {
        return super.isDoneAndFailed();
    }

    @Override
    public boolean whenSuccess(long timeout) {
        super.setThread(Thread.currentThread());

        if (!isDone()) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(timeout));
        }
        if (!isDone()) {
            receive(new ConnectionException.RequestTimeout("Request timeout"),
                    CauseType.CONNECTION_REQUEST_TIMEOUT);
        }
        return isDoneAndSuccess();
    }

    @Override
    public CauseType getCauseType() {
        return super.getCauseType();
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

    @Override
    public Throwable getCauseAndReset() {
        Throwable throwable = super.getCause();
        reset();
        return throwable;
    }

    @Override
    public String getEntity() {
        return super.getEntity();
    }

    @Override
    public String getEntityAndReset() {
        String entity = super.getEntity();
        reset();
        return entity;
    }

    @Override
    public ResponseFuture<String> addListener(FutureListener<String> listener) {
        return null;//TODO
    }

    @Override
    public ResponseFuture<String> setFallBack(FallBackMethod<String> fallBackMethod) {
        return null;//TODO
    }

    @Override
    public boolean reset() {
        if (super.reset()) {
            ChannelManager.release(holder);
            return true;
        } else {
            ChannelManager.close(holder);
            throw new UnexpectedException();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return super.isCancelled();
    }

    @Override
    public boolean isDone() {
        return super.isDone();
    }

    @Override
    public String get() {
        return super.get();
    }

    @Override
    public String get(long timeout, TimeUnit timeUnit) throws TimeoutException, ExecutionException {
        return super.get(timeout, timeUnit);
    }

    @Override
    public void setPayload(ChannelResponsePromise responsePromise, ResponseDecoder<String> decoder) {
        throw new UnsupportedOperationException();
    }
}

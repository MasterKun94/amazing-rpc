package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import httpService.proxy.FallBackMethod;
import httpService.proxy.ResponseDecoder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultResponsePromise<T> extends AbstractResponsePromise<T> {
    private ChannelResponsePromise channelPromise;
    private ResponseDecoder<T> decoder;

    @Override
    public void setPayload(ChannelResponsePromise promise, ResponseDecoder<T> decoder) {
        this.channelPromise = promise;
        this.decoder = decoder;
    }

    @Override
    public void receive(T entity) {
        super.receive(entity);
    }

    @Override
    public void receive(Throwable cause, CauseType type) {
        super.receive(cause, type);
    }

    @Override
    public void receive(T entity, Throwable cause, CauseType type) {
        super.receive(entity, cause, type);
    }

    @Override
    public boolean isDoneAndSuccess() {
        if (isDone()) {
            return super.isDoneAndSuccess();
        }
        return false;
    }

    @Override
    public boolean isDoneAndFailed() {
        if (isDone()) {
            return super.isDoneAndFailed();
        }
        return false;
    }

    @Override
    public boolean whenSuccess(long timeout) {
        if (isDone()) {
            return super.isDoneAndSuccess();
        }
        if (channelPromise == null) {
            throw new IllegalArgumentException("Need ChannelResponsePromise");//TODO
        }
        channelPromise.whenSuccess(timeout);
        return super.isDoneAndSuccess();
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
        return super.getCauseAndReset();
    }

    @Override
    public T getEntity() {
        return super.getEntity();
    }

    @Override
    public T getEntityAndReset() {
        return super.getEntityAndReset();
    }

    @Override
    public ResponseFuture<T> addListener(FutureListener<T> listener) {
        return null;//TODO
    }

    @Override
    public ResponseFuture<T> setFallBack(FallBackMethod<T> fallBackMethod) {
        return null;
    }

    @Override
    public boolean reset() {
        return super.reset();
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
        return flushPromiseAndCheckIsDone();
    }

    @Override
    public T get() {
        return super.get();
    }

    @Override
    public T get(long timeout, TimeUnit timeUnit) throws TimeoutException, ExecutionException {
        return super.get(timeout, timeUnit);
    }

    private boolean flushPromiseAndCheckIsDone() {
        if (super.isDone()) {
            return true;
        }
        if (channelPromise == null) {
            return false;
        }
        if (channelPromise.isDoneAndSuccess()) {
            T t;
            try {
                t = decoder.decode(channelPromise.getEntity());
                receive(t);
                return true;
            } catch (Throwable cause) {
                receive(cause, CauseType.DECODE_FAILED);
                return true;
            } finally {
                channelPromise.reset();
            }
        }
        if (channelPromise.isDoneAndFailed()) {
            try {
                receive(channelPromise.getCause(), channelPromise.getCauseType());
                return true;
            } finally {
                channelPromise.reset();
            }
        }
        return false;
    }
}

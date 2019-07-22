package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import httpService.exceptions.UnexpectedException;
import httpService.proxy.FallBackMethod;
import httpService.proxy.ResponseDecoder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractResponsePromise<T> implements ResponsePromise<T> {

    private volatile T entity;
    private volatile Throwable cause;
    private volatile CauseType causeType;
    private volatile boolean isSuccess;
    private volatile Thread thread;

    private AtomicBoolean isDone = new AtomicBoolean(false);

    @Override
    public void receive(T entity) {
        this.entity = entity;
        this.isSuccess = true;
        if (!this.isDone.compareAndSet(false, true))
            throw new UnexpectedException();
        LockSupport.unpark(thread);
    }

    @Override
    public void receive(Throwable cause, CauseType type) {
        this.cause = cause;
        this.causeType = type;
        this.isSuccess = false;
        if (!this.isDone.compareAndSet(false, true))
            throw new UnexpectedException();
        LockSupport.unpark(thread);
    }

    @Override
    public void receive(T entity, Throwable cause, CauseType type) {
        this.entity = entity;
        receive(cause, type);
    }

    @Override
    public boolean isDoneAndSuccess() {
        return isDone() && isSuccess;
    }

    @Override
    public boolean isDoneAndFailed() {
        return isDone() && !isSuccess;
    }

    @Override
    public abstract boolean whenSuccess(long timeout);

    @Override
    public CauseType getCauseType() {
        return causeType;
    }

    @Override
    public Throwable getCause() {
        if (isDone()) {
            return this.cause;
        } else {
            throw new UnexpectedException();
        }
    }

    @Override
    public Throwable getCauseAndReset() {
        try {
            return getCause();
        } finally {
            reset();
        }
    }

    @Override
    public T getEntity() {
        if (isDone()) {
            return this.entity;
        } else {
            throw new UnexpectedException();
        }
    }

    @Override
    public T getEntityAndReset() {
        try {
            return getEntity();
        } finally {
            reset();
        }
    }

    @Override
    public abstract ResponseFuture<T> addListener(FutureListener<T> listener);

    @Override
    public abstract ResponseFuture<T> setFallBack(FallBackMethod<T> fallBackMethod);

    @Override
    public boolean reset() {
        this.causeType = null;
        this.cause = null;
        this.isSuccess = false;
        this.thread = null;
        this.entity = null;
        return this.isDone.compareAndSet(true, false);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isDone.get();
    }

    @Override
    public T get() {
        this.thread = Thread.currentThread();
        if (!isDone()) {
            whenSuccess(TimeUnit.SECONDS.toNanos(20));
        }
        return entity;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException, ExecutionException {
        if (!isDone()) {
            whenSuccess(unit.toMillis(timeout));
        }
        if (isDoneAndSuccess()) {
            return entity;
        } else if (isDoneAndFailed()) {
            throw new ExecutionException(cause);
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public abstract void setPayload(
            ChannelResponsePromise responsePromise,
            ResponseDecoder<T> decoder);

    void setThread(Thread thread) {
        this.thread = thread;
    }
}

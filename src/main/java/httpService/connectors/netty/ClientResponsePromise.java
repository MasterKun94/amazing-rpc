package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import httpService.exceptions.ConnectionException;
import httpService.exceptions.UnexpectedException;
import httpService.exceptions.UnhandledException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class ClientResponsePromise<T> implements ResponsePromise<T> {
    private volatile T entity;
    private volatile Throwable cause;
    private volatile CauseType causeType;
    private volatile boolean isSuccess;
    private volatile Thread thread;
    private final ConcurrentLinkedQueue<FutureListener<T>> listeners = new ConcurrentLinkedQueue<>();

    private AtomicBoolean isDone = new AtomicBoolean(false);

    @Override
    public boolean receive(T entity) {
        this.entity = entity;
        this.isSuccess = true;
        return receive();
    }

    @Override
    public boolean receive(Throwable cause, CauseType type) {
        this.cause = cause;
        this.causeType = type;
        this.isSuccess = false;
        return receive();
    }

    @Override
    public boolean receive(T entity, Throwable cause, CauseType type) {
        this.entity = entity;
        this.cause = cause;
        this.causeType = type;
        this.isSuccess = false;
        return receive();
    }

    @Override
    public void setEntity(T entity) {
        this.entity = entity;
    }

    @Override
    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public void setCauseType(CauseType type) {
        this.causeType = type;
    }

    @Override
    public boolean setSuccess(boolean isSuccess) {
        boolean before = this.isSuccess;
        this.isSuccess = isSuccess;
        return before;
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
    public boolean whenSuccess(long timeout) {
        this.thread = Thread.currentThread();

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
    public ResponseFuture<T> addListener(FutureListener<T> listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public ResponseFuture<T> addFallBackMethod(FallBackMethod<T> fallBackMethod) {
        FutureListener<T> listener = promise -> {
            if (isDoneAndFailed()) {
                this.entity = fallBackMethod.apply(cause, causeType);
                this.isSuccess = true;
            }
        };
        addListener(listener);
        return this;
    }

    @Override
    public boolean reset() {
        this.causeType = null;
        this.cause = null;
        this.isSuccess = false;
        this.thread = null;
        this.entity = null;
        this.listeners.clear();
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
        if (isDoneAndFailed()) {
            throw new UnhandledException(cause, causeType);
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
            throw new UnhandledException(cause, causeType);
        } else {
            throw new TimeoutException();
        }
    }

    private boolean receive() {
        if (!this.isDone.compareAndSet(false, true)) {
            return false;
        }
        while (!listeners.isEmpty()) {
            listeners.poll().listen(this);
        }
        LockSupport.unpark(thread);
        return true;
    }
}

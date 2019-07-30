package httpService.proxy;

import httpService.exceptions.CauseType;
import httpService.exceptions.UnexpectedException;
import pool.ReleaseAble;

import java.util.concurrent.TimeUnit;

public class AutoResetChannelPromise extends ClientResponsePromise<String> {

    private final ReleaseAble holder;

    public AutoResetChannelPromise(ReleaseAble holder) {
        this.holder = holder;
    }

    @Override
    public boolean receive(String entity) {
        super.receive(entity);
        return reset();
    }

    @Override
    public boolean receive(Throwable cause, CauseType type) {
        super.receive(cause, type);
        return reset();
    }

    @Override
    public boolean receive(String entity, Throwable cause, CauseType type) {
        super.receive(entity, cause, type);
        return reset();
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
        throw new UnsupportedOperationException();
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
        try {
            return getCause();
        } finally {
            reset();
        }
    }

    @Override
    public String getEntity() {
        return super.getEntity();
    }

    @Override
    public String getEntityAndReset() {
        try {
            return getEntity();
        } finally {
            reset();
        }
    }

    @Override
    public ResponseFuture<String> addListener(FutureListener<String> listener) {
        return super.addListener(listener);
    }

    @Override
    public boolean reset() {
        try {
            if (super.reset()) {
                return true;
            } else {
                holder.close();
                throw new UnexpectedException();
            }
        } finally {
            holder.release();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return super.isDone();
    }

    @Override
    public String get() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(long timeout, TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }
}

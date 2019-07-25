package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import httpService.exceptions.UnexpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.ChannelHolder;
import pool.ChannelManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AutoResetChannelPromise extends ClientResponsePromise<String> {

    private final ChannelHolder holder;

    public AutoResetChannelPromise(ChannelHolder holder) {
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
    public void setEntity(String entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCause(Throwable cause) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCauseType(CauseType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setSuccess(boolean isSuccess) {
        throw new UnsupportedOperationException();
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
                ChannelManager.close(holder);
                throw new UnexpectedException();
            }
        } finally {
            ChannelManager.release(holder);
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
    public String get(long timeout, TimeUnit timeUnit) throws TimeoutException, ExecutionException {
        throw new UnsupportedOperationException();
    }
}

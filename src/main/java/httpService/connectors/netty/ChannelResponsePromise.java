package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import httpService.exceptions.UnexpectedException;
import pool.ChannelHolder;
import pool.ChannelManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChannelResponsePromise extends ClientResponsePromise<String> {

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
        return super.whenSuccess(timeout);
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
}

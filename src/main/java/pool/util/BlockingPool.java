package pool.util;

public interface BlockingPool<T> extends Pool<T> {
    T get() throws InterruptedException;

    int getIndex() throws InterruptedException;

    int waitingCapacity();
}

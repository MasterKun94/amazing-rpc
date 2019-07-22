package pool.util;

public interface ElementInitializer<T> {
    T get() throws Exception;
}

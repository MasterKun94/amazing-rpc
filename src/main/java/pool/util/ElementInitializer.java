package pool.util;

public interface ElementInitializer<T> {
    T init() throws Exception;
}

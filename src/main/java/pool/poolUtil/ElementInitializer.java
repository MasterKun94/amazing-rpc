package pool.poolUtil;

public interface ElementInitializer<T> {
    T init() throws Exception;
}

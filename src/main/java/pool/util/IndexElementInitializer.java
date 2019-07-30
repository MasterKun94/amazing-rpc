package pool.util;

public interface IndexElementInitializer<T> {
    T init(int i) throws Exception;
}

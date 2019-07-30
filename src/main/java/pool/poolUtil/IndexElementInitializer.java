package pool.poolUtil;

public interface IndexElementInitializer<T> {
    T init(int i) throws Exception;
}

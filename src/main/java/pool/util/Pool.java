package pool.util;

public interface Pool<T> {
    /**
     * 从对象池中请求一个对象，如果对象池内存在闲置的对象，（引用计数为0 的对象），则成功返回，
     * 并且该对象在池内的引用计数变为1，若对象池内所有对象都被引用，那么返回null值。
     *
     * @return 返回一个对象池内的对象
     */
    T borrow();

    int borrowIndex();

    /**
     * 从对象池中请求一个对象，如果对象池内存在闲置的对象，（引用计数为0 的对象），则成功返回，
     * 并且该对象在池内的引用计数变为1，若对象池内所有对象都被引用，那么抛出
     * {@code IllegalStateException} 对象池已满错误：
     *
     *
     * @return 返回一个对象池内的对象
     */
    T request();

    /**
     * 为该参数对象的引用计数增加1，该对象必须为对象内的对象，不然会抛出
     * {@code IllegalArgumentException} 错误
     *
     * @param t 请求引用计数+1 的对象
     * @return 返回增加后的引用计数
     */
    int addReference(T t);

    int addReference(int pointer);

    int release(T t);

    int release(int pointer);

    int getCounter(T t);

    int getCounter(int pointer);

    int getPointer(T t);

    T getElement(int pointer);

    int availableAmount();

    boolean isFull();

    int size();
}

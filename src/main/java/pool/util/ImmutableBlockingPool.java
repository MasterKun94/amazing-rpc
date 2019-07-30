package pool.util;

import java.util.concurrent.BlockingQueue;

public class ImmutableBlockingPool<T> extends ImmutablePool<T> implements BlockingPool<T> {
    private final BlockingQueue<Box> blockingQueue;

    ImmutableBlockingPool(int capacity, Class<T> clazz, BlockingQueue<Box> blockingQueue) {
        super(capacity, clazz);
        this.blockingQueue = blockingQueue;
    }

    ImmutableBlockingPool(int capacity, IndexElementInitializer<T> init, BlockingQueue<Box> blockingQueue) {
        super(capacity, init);
        this.blockingQueue = blockingQueue;
    }

    @Override
    public T get() throws InterruptedException {
        int index = getIndex();
        return index == -1 ? null : getElement(index);
    }

    @Override
    public int getIndex() throws InterruptedException {
        Box box = null;
        int t;
        do {
            while (!isFull()) {
                t = borrowIndex();
                if (t != -1) {
                    return t;
                }
            }
            if (box == null) {
                box = Box.emptyBox();
            } else {
                box.clean();
            }
            if (isFull()) {
                blockingQueue.add(box);
            } else {
                box.sendBack();
            }
            while (box.isEmpty()) {
                box.wait();
            }
        } while (box.isSendBack());

        return box.getPayload();
    }

    @Override
    public int waitingCapacity() {
        return blockingQueue.size();
    }

    @Override
    public int release(T t) {
        int index = getPointer(t);
        if (getCounter(index) == 1) {
            Box box = blockingQueue.poll();
            if (box != null) {
                box.setPayload(index);
                box.notify();
                return 0;
            }
        }
        int count = release(index);
        if (count == 0) {
            Box box = blockingQueue.poll();
            if (box != null) {
                box.sendBack();
                box.notify();
            }
        }
        return count;
    }
}

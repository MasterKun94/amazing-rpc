package pool;

import httpService.DefaultArgs;
import pool.util.BlockingPool;
import pool.util.Box;
import pool.util.ElementInitializer;
import pool.util.Pools;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class ChannelPool implements BlockingPool<ChannelHolder> {
    private final BlockingPool<ChannelHolder> pool;

    public ChannelPool(
            String host,
            int port,
            int capacity,
            boolean lazy,
            Map<String, String> defaultHeaders,
            BlockingQueue<Box> queue,
            boolean showRequest,
            boolean showResponse
    ) {
        DefaultArgs args = new DefaultArgs(host, port, defaultHeaders);
        ElementInitializer<ChannelHolder> init = () -> new ChannelHolder(args, lazy, showRequest, showResponse);
        pool = Pools.immutableBlockingPool(capacity, init, queue);
    }

    @Override
    public ChannelHolder get() throws InterruptedException {
        return pool.get();
    }

    @Override
    public int getIndex() throws InterruptedException {
        return pool.getIndex();
    }

    @Override
    public int waitingCapacity() {
        return pool.waitingCapacity();
    }

    @Override
    public ChannelHolder borrow() {
        return pool.borrow();
    }

    @Override
    public int borrowIndex() {
        return pool.borrowIndex();
    }

    @Override
    public ChannelHolder request() {
        return pool.request();
    }

    @Override
    public int addReference(ChannelHolder channel) {
        return pool.addReference(channel);
    }

    @Override
    public int addReference(int pointer) {
        return pool.addReference(pointer);
    }

    @Override
    public int release(ChannelHolder channel) {
        return pool.release(channel);
    }

    @Override
    public int release(int pointer) {
        return pool.release(pointer);
    }

    @Override
    public int getCounter(ChannelHolder channel) {
        return pool.getCounter(channel);
    }

    @Override
    public int getCounter(int pointer) {
        return pool.getCounter(pointer);
    }

    @Override
    public int getPointer(ChannelHolder channel) {
        return pool.getPointer(channel);
    }

    @Override
    public ChannelHolder getElement(int pointer) {
        ChannelHolder holder = pool.getElement(pointer);
        holder.setPoolIndex(pointer);
        return holder;
    }

    @Override
    public int availableAmount() {
        return pool.availableAmount();
    }

    @Override
    public boolean isFull() {
        return pool.isFull();
    }

    @Override
    public int size() {
        return pool.size();
    }

    public String toString() {
        return "ChannelPool(size: " + size() +
                ", available: " + availableAmount() +
                ", waiting capacity: " + waitingCapacity() + ")";
    }
}

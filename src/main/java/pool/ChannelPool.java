package pool;

import httpService.proxy.ChannelHolderConnector;
import httpService.proxy.DefaultArgs;
import httpService.connectors.Connector;
import io.netty.handler.ssl.SslContext;
import pool.poolUtil.*;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;

public class ChannelPool implements BlockingPool<Connector> {
    private final BlockingPool<Connector> pool;

    public ChannelPool(
            InetSocketAddress address,
            int capacity,
            boolean lazy,
            SslContext sslContext,
            String[][] defaultHeaders,
            BlockingQueue<Box> queue,
            boolean showReq,
            boolean showRes) {
        DefaultArgs args = new DefaultArgs(address, defaultHeaders);
        IndexElementInitializer<Connector> init = (index) ->
            new ChannelHolderConnector(args, sslContext, lazy, showReq, showRes, index);

        pool = Pools.immutableBlockingPool(capacity, init, queue);
    }

    @Override
    public Connector get() throws InterruptedException {
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
    public Connector borrow() {
        return pool.borrow();
    }

    @Override
    public int borrowIndex() {
        return pool.borrowIndex();
    }

    @Override
    public Connector request() {
        return pool.request();
    }

    @Override
    public int addReference(Connector channel) {
        return pool.addReference(channel);
    }

    @Override
    public int addReference(int pointer) {
        return pool.addReference(pointer);
    }

    @Override
    public int release(Connector channel) {
        return pool.release(channel);
    }

    @Override
    public int release(int pointer) {
        return pool.release(pointer);
    }

    @Override
    public int getCounter(Connector channel) {
        return pool.getCounter(channel);
    }

    @Override
    public int getCounter(int pointer) {
        return pool.getCounter(pointer);
    }

    @Override
    public int getPointer(Connector channel) {
        return pool.getPointer(channel);
    }

    @Override
    public Connector getElement(int pointer) {
        return pool.getElement(pointer);
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

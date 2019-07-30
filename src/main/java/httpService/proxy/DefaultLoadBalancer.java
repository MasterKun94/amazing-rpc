package httpService.proxy;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultLoadBalancer implements LoadBalancer {
    private List<InetSocketAddress> socketAddresses;
    private AtomicInteger index;
    private final int size;

    public DefaultLoadBalancer(List<InetSocketAddress> socketAddresses) {
        this.socketAddresses = socketAddresses;
        this.index = new AtomicInteger();
        this.size = socketAddresses.size();
    }

    @Override
    public InetSocketAddress select() {
        int i = index.getAndIncrement();
        return socketAddresses.get(i % size);
    }
}

package httpService.connectors;

import httpService.proxy.SocketAddress;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultLoadBalancer implements LoadBalancer {
    private List<SocketAddress> socketAddresses;
    private AtomicInteger index;
    private final int size;

    public DefaultLoadBalancer(List<SocketAddress> socketAddresses) {
        this.socketAddresses = socketAddresses;
        this.index = new AtomicInteger();
        this.size = socketAddresses.size();
    }

    @Override
    public SocketAddress select() {
        int i = index.getAndIncrement();
        return socketAddresses.get(i % size);
    }
}

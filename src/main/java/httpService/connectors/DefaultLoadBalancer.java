package httpService.connectors;

import httpService.proxy.Host;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultLoadBalancer implements LoadBalancer {
    private List<Host> hosts;
    private AtomicInteger index;
    private final int size;

    public DefaultLoadBalancer(List<Host> hosts) {
        this.hosts = hosts;
        this.index = new AtomicInteger();
        this.size = hosts.size();
    }

    @Override
    public Host select() {
        int i = index.getAndIncrement();
        return hosts.get(i % size);
    }
}

package httpService.proxy;

import java.net.InetSocketAddress;

public interface LoadBalancer {
    InetSocketAddress select();
}

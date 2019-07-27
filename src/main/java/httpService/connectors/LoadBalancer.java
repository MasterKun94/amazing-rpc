package httpService.connectors;

import httpService.proxy.SocketAddress;

public interface LoadBalancer {
    SocketAddress select();
}

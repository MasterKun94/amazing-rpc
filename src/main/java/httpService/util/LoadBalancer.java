package httpService.util;

import java.net.InetSocketAddress;

public interface LoadBalancer {
    InetSocketAddress select();
}

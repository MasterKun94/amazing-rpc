package httpService.util;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalancerInitializer {
    LoadBalancer init(List<InetSocketAddress> addresses);
}

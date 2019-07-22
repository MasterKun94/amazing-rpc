package httpService.connectors;

import httpService.proxy.Host;

public interface LoadBalancer {
    Host select();
}

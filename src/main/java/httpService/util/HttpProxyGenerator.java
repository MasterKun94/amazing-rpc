package httpService.util;

import httpService.connection.RpcClient;

public interface HttpProxyGenerator {

    static HttpProxyGenerator start() {
        return new HttpProxyGeneratorImpl();
    }

    HttpProxyGenerator addServiceParser(ServiceParser parser);

    HttpProxyGenerator addInterceptor(DecoratorInitializer initializer);

    HttpProxyGenerator setConfig(ServiceConfig config);

    HttpProxyGenerator setLoadBalancer(LoadBalancerInitializer initializer);

    RpcClient getClient();

    <T> T getProxy(Class<T> clazz);

    <T> T getProxy(Class<T> clazz, T fallBack);
}

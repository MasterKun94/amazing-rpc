package httpService.util;

public interface HttpProxyGenerator {

    static HttpProxyGenerator start() {
        return new HttpProxyGeneratorImpl();
    }

    HttpProxyGenerator addServiceParser(ServiceParser parser);

    HttpProxyGenerator addInterceptor(DecoratorInitializer initializer);

    HttpProxyGenerator setConfig(ServiceConfig config);

    HttpProxyGenerator setLoadBalancer(LoadBalancerInitializer initializer);

    <T> T getProxy(Class<T> clazz);
}

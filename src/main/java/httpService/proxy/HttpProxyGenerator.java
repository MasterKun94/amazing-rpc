package httpService.proxy;

public interface HttpProxyGenerator {

    static HttpProxyGenerator start() {
        return new HttpProxyGeneratorImpl();
    }

    HttpProxyGenerator addServiceParser(ServiceParser parser);

    HttpProxyGenerator addInterceptor(MonitorInitializer initializer);

    <T> T getProxy(Class<T> clazz);
}

package httpService.util;

import httpService.annotation.ServiceContext;
import httpService.connectors.Connector;
import httpService.connectors.ConnectorBuilder;
import httpService.connectors.ConnectorType;
import httpService.ssl.SslContextParser;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings("unchecked")
public class HttpProxyGeneratorImpl implements HttpProxyGenerator {

    private List<ServiceParser> parsers = new ArrayList<>();
    private List<DecoratorInitializer> monitorInits = new ArrayList<>();
    private ServiceConfig config;
    private LoadBalancerInitializer balancerInit;

    HttpProxyGeneratorImpl() { }

    public <T> T getProxy(Class<T> clazz) {
        parsers.add(new DefaultServiceParser());
        config = setDefaultIfAbsent(clazz);
        balancerInit = setDefaultIfAbsent();
        List<InetSocketAddress> socketAddresses = parseAddress(config, parsers);
        LoadBalancer balancer = balancerInit.init(socketAddresses);
        Connector connector = getConnector(config, socketAddresses, monitorInits);
        Map<Method, ProxyMethod> methodMap = new HashMap<>();
        try {
            for (Method method : clazz.getMethods()) {
                ProxyMethod proxyMethod = Methods.of(method)
                        .setBalancer(balancer)
                        .setConfig(config)
                        .setConnector(connector)
                        .build();
                methodMap.put(method, proxyMethod);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[]{clazz},
                (proxy, method, args) -> methodMap.get(method).apply(args));
    }

    @Override
    public HttpProxyGenerator addServiceParser(ServiceParser parser) {
        this.parsers.add(parser);
        return this;
    }

    @Override
    public HttpProxyGenerator addInterceptor(DecoratorInitializer initializer) {
        this.monitorInits.add(initializer);
        return this;
    }

    @Override
    public HttpProxyGenerator setConfig(ServiceConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public HttpProxyGenerator setLoadBalancer(LoadBalancerInitializer initializer) {
        this.balancerInit = initializer;
        return this;
    }

    private <T> ServiceConfig setDefaultIfAbsent(Class<T> clazz) {
        if (config == null) {
            if (clazz.isAnnotationPresent(ServiceContext.class)) {
                ServiceContext context = clazz.getAnnotation(ServiceContext.class);
                this.config = ServiceConfig.parseContext(context);
            } else {
                //TODO
                throw new IllegalArgumentException("Annotation ServiceContext not found");
            }
        }
        return this.config;
    }

    private LoadBalancerInitializer setDefaultIfAbsent() {
        if (balancerInit == null) {
            balancerInit = DefaultLoadBalancer::new;
        }
        return balancerInit;
    }

    private static List<InetSocketAddress> parseAddress(ServiceConfig config,
                                                        List<ServiceParser> parsers) {
        List<InetSocketAddress> socketAddresses = null;
        for (ServiceParser parser : parsers) {
            socketAddresses = parser.parse(config.getHost());
            if (socketAddresses != null)
                break;
        }
        if (socketAddresses == null) {
            throw new IllegalArgumentException("wrong service name");
        }
        return socketAddresses;
    }


    private static Connector getConnector(ServiceConfig config,
                                       List<InetSocketAddress> addresses,
                                       List<DecoratorInitializer> initializers) {

        if (config.getConnector() == ConnectorType.NETTY) {
            return ConnectorBuilder
                    .createNetty(addresses)
                    .setPoolCapacity(config.getPoolCapacity())
                    .setLazyInit(config.isLazyInit())
                    .setShowRequest(config.isShowRequest())
                    .setShowResponse(config.isShowResponse())
                    .setHeaders(config.getDefaultHeaders())
                    .setSslContext(SslContextParser.get(config.getSslConfig()))
                    .setMonitors(initializers)
                    .setQueue(new LinkedBlockingQueue<>())
                    .build();
        } else {
            return ConnectorBuilder
                    .createHttpClient()
                    .build();
        }

    }
}

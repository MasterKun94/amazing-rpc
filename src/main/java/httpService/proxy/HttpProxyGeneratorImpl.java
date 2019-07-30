package httpService.proxy;

import httpService.annotation.RequestHeaders;
import httpService.annotation.ServiceContext;
import httpService.connectors.Connector;
import httpService.connectors.ConnectorBuilder;
import httpService.connectors.ConnectorType;
import httpService.ssl.SslContextParser;
import io.netty.handler.ssl.SslContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import static httpService.proxy.AliasUtil.parse;

@SuppressWarnings("unchecked")
public class HttpProxyGeneratorImpl implements HttpProxyGenerator {

    private final List<ServiceParser> parsers = new ArrayList<>();
    private final List<MonitorInitializer> initializers = new ArrayList<>();

    HttpProxyGeneratorImpl() { }

    public <T> T getProxy(Class<T> clazz) {
        parsers.add(new DefaultServiceParser());
        ServiceContext context;
        if (clazz.isAnnotationPresent(ServiceContext.class)) {
            context = clazz.getAnnotation(ServiceContext.class);
        } else {
            //TODO
            throw new IllegalArgumentException("Annotation ServiceContext not found");
        }

        List<InetSocketAddress> socketAddresses = socketAddresses(context, parsers);
        LoadBalancer balancer = new DefaultLoadBalancer(socketAddresses);
        String contentPath = parse(context, "path");
        long timeout = parse(context, "timeout");
        Connector connector = getConnector(context, socketAddresses, initializers);

        InvocationHandler invocationHandler = new InvocationHandler() {
            private Map<Method, ProxyMethod> methodMap = new HashMap<>();

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                ProxyMethod proxyMethod = methodMap.get(method);
                if (proxyMethod == null) {
                    proxyMethod = Methods.of(method)
                            .setEncodeArgs(contentPath, balancer)
                            .setConnector(connector)
                            .build(timeout);
                    methodMap.put(method, proxyMethod);
                }
                return proxyMethod.apply(args);
            }
        };

        return (T) Proxy.newProxyInstance(
                invocationHandler.getClass().getClassLoader(),
                new Class[]{clazz},
                invocationHandler);
    }

    public HttpProxyGeneratorImpl addServiceParser(ServiceParser parser) {
        this.parsers.add(parser);
        return this;
    }

    public HttpProxyGeneratorImpl addInterceptor(MonitorInitializer initializer) {
        this.initializers.add(initializer);
        return this;
    }

    private static List<InetSocketAddress> socketAddresses(ServiceContext serviceContext, List<ServiceParser> parsers) {
        List<InetSocketAddress> socketAddresses = null;
        for (ServiceParser parser : parsers) {
            socketAddresses = parser.parse(parse(serviceContext, "host"));
            if (socketAddresses != null)
                break;
        }
        if (socketAddresses == null) {
            throw new IllegalArgumentException("wrong service name");
        }
        return socketAddresses;
    }


    private static Connector getConnector(ServiceContext context,
                                       List<InetSocketAddress> addresses,
                                       List<MonitorInitializer> initializers) {

        SslContext sslContext = SslContextParser.get(context.sslConfig());
        int capacity = parse(context, "poolCapacity");
        boolean lazy = parse(context, "lazyInit");
        boolean showRequest = parse(context, "showRequest");
        boolean showResponse = parse(context, "showResponse");
        ConnectorType type = parse(context, "connector");

        RequestHeaders[] heads = parse(context, "defaultHeaders");
        Map<String, String> defaultHeaders = new HashMap<>(heads.length);
        for (RequestHeaders head : heads) {
            String name = parse(head, "name");
            String value = parse(head, "defaultValue");
            defaultHeaders.put(name, value);
        }
        if (type == ConnectorType.NETTY) {
            return ConnectorBuilder
                    .createNetty(addresses)
                    .setPoolCapacity(capacity)
                    .setLazyInit(lazy)
                    .setShowRequest(showRequest)
                    .setShowResponse(showResponse)
                    .setQueue(new LinkedBlockingQueue<>())
                    .setHeaders(defaultHeaders)
                    .setSslContext(sslContext)
                    .setMonitors(initializers)
                    .build();
        } else {
            return ConnectorBuilder
                    .createHttpClient()
                    .build();
        }

    }
}

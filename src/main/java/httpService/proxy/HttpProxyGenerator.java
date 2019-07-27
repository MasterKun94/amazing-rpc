package httpService.proxy;

import httpService.annotation.*;
import httpService.connectors.*;
import httpService.connectors.netty.ResponsePromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import static httpService.util.AliasUtil.*;

/**
 * http请求代理生成器，
 */
@SuppressWarnings("unchecked")
public class HttpProxyGenerator<T> {
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyGenerator.class);

    private Connector connector;
    private String contentPath;
    private long timeout;
    private T proxy;
    private LoadBalancer balancer;
    private List<ServiceParser> parsers;


    /**
     * 实例化一个代理生成器对象，如果被代理的接口有方法返回{@code Future}类型，建议使用构造函数
     * {@code HttpProxyGenerator(ExecutorService executor)}
     */
    private HttpProxyGenerator(Class<T> clazz) {
        parsers = new ArrayList<>();
        parsers.add(new DefaultServiceParser());
        boolean lazy;
        boolean showRequest;
        boolean showResponse;
        int capacity;
        ConnectorType type;
        Map<String, String> defaultHeaders;
        List<SocketAddress> socketAddresses = null;
        if (clazz.isAnnotationPresent(ServiceContext.class)) {
            ServiceContext serviceContext = clazz.getAnnotation(ServiceContext.class);
            for (ServiceParser parser : parsers) {
                socketAddresses = parser.parse(parse(serviceContext, "host"));
                if (socketAddresses != null)
                    break;
            }
            if (socketAddresses == null) {
                throw new IllegalArgumentException("wrong service name");
            }
            balancer = new DefaultLoadBalancer(socketAddresses);
            this.contentPath = parse(serviceContext, "path");
            this.timeout = parse(serviceContext, "timeout");

            capacity = parse(serviceContext, "poolCapacity");
            lazy = parse(serviceContext, "lazyInit");
            showRequest = parse(serviceContext, "showRequest");
            showResponse = parse(serviceContext, "showResponse");
            type = parse(serviceContext, "connector");

            RequestHeaders[] heads = parse(serviceContext, "defaultHeaders");
            defaultHeaders = new HashMap<>(heads.length);
            for (RequestHeaders head : heads) {
                String name = parse(head, "name");
                String value = parse(head, "defaultValue");
                defaultHeaders.put(name, value);
            }
        } else {
            throw new IllegalArgumentException("Annotation ServiceContext not found");
        }

        if (type == ConnectorType.NETTY) {
            this.connector = ConnectorBuilder
                    .createNetty(socketAddresses)
                    .setPoolCapacity(capacity)
                    .setLazyInit(lazy)
                    .setShowRequest(showRequest)
                    .setShowResponse(showResponse)
                    .setQueue(new LinkedBlockingQueue<>())
                    .setHeaders(defaultHeaders)
                    .build();
        } else {
            this.connector = ConnectorBuilder
                    .createHttpClient()
                    .build();
        }
        Class[] classes = new Class[]{clazz};
        InvocationHandler httpProxy = new HttpProxy();
        proxy = (T) Proxy.newProxyInstance(
                HttpProxy.class.getClassLoader(),
                classes,
                httpProxy);
    }

    /**
     * 生成一个代理对象，参数是一个接口，且通过{@link httpService.annotation} 中的注解标注
     * 请求类型，url，参数，头部信息，请求体等信息代理生成器会根据注解自动生成一个代理的对象
     *
     * @return 一个实例化的代理对象
     */
    public T getProxy() {
        return this.proxy;
    }

    public static <T> HttpProxyGenerator<T> start(Class<T> clazz) {
        return new HttpProxyGenerator<>(clazz);
    }

    public HttpProxyGenerator<T> addServiceParser(ServiceParser parser) {
        this.parsers.add(parser);
        return this;
    }

    private class HttpProxy implements InvocationHandler {
        private Map<Method, ProxyMethod> methodMap = new HashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            ProxyMethod proxyMethod = methodMap.get(method);
            if (proxyMethod == null) {
                proxyMethod = init(method);
                methodMap.put(method, proxyMethod);
            }
            return proxyMethod.apply(args);
        }

        //第一次调对象的某个方法时会调用该方法，并将该方法返回的函数放入{@code methodMap}中，
        private ProxyMethod init(Method method) throws ClassNotFoundException {
            RequestEncoder encoder = Methods.encode(method, contentPath, balancer, timeout);
            ResponseDecoder decoder = Methods.decode(method);
            ArgsToPromise arg2Promise = Methods.argsToPromise(encoder, connector, decoder);
            return Methods.asyncProxyMethod(arg2Promise, timeout, method);
        }
    }
}

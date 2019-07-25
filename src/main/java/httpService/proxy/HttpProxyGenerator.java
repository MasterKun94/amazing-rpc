package httpService.proxy;

import com.alibaba.fastjson.JSON;
import httpService.HttpMethod;
import httpService.RequestArgs;
import httpService.annotation.*;
import httpService.connectors.*;
import httpService.connectors.netty.FallBackMethod;
import httpService.connectors.netty.ResponsePromise;
import httpService.exceptions.CauseType;
import httpService.util.UrlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

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
    private T fallBack;
    private Map<Method, FallBackMethod> fallBackMethodMap;//TODO
    private LoadBalancer loadBalancer;

    /**
     * 实例化一个代理生成器对象，如果被代理的接口有方法返回{@code Future}类型，建议使用构造函数
     * {@code HttpProxyGenerator(ExecutorService executor)}
     */
    private HttpProxyGenerator(Class<T> clazz) {
        int capacity;
        boolean lazy;
        boolean showRequest;
        boolean showResponse;
        ConnectorType type;
        Map<String, String> defaultHeaders;
        List<Host> hosts;
        if (clazz.isAnnotationPresent(ServiceContext.class)) {
            ServiceContext serviceContext = clazz.getAnnotation(ServiceContext.class);

            hosts = ServiceParser.parse(parse(serviceContext, "host"));
            loadBalancer = new DefaultLoadBalancer(hosts);
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
                    .createNetty(hosts)
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
        this.fallBackMethodMap = new HashMap<>(clazz.getMethods().length);
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
    public static <T> T getProxy(Class<T> clazz) {
        HttpProxyGenerator<T> generator = new HttpProxyGenerator<>(clazz);
        return generator.proxy;
    }

    public void setFallBack(T fallBackObject) {
        this.fallBack = fallBackObject;
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
            RequestEncoder encoder = getRequestEncoder(method);
            ResponseDecoder decoder = getResponseDecoder(method);
            ArgsToPromise arg2Promise = Methods.argsToPromise(encoder, connector, decoder);
            return Methods.asyncProxyMethod(arg2Promise, timeout, method.getReturnType());
        }

        private RequestEncoder getRequestEncoder(Method method) {
            String tailUrl;
            HttpMethod httpMethod;
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                httpMethod = requestMapping.method();
                tailUrl = parse(requestMapping, "path");
                if (!tailUrl.startsWith("/")) {
                    tailUrl = "/" + tailUrl;
                }
            } else {
                String methodName = method.getName();
                if (methodName.startsWith("get")) {
                    httpMethod = HttpMethod.GET;
                } else if (methodName.startsWith("post")) {
                    httpMethod = HttpMethod.POST;
                } else if (methodName.startsWith("put")) {
                    httpMethod = HttpMethod.PUT;
                } else if (methodName.startsWith("delete")) {
                    httpMethod = HttpMethod.DELETE;
                } else {
                    throw new IllegalArgumentException("无法解析 " + methodName + " 的url");
                }
                int index = httpMethod.name().length();
                char ch = methodName.charAt(index);
                if (ch >= 'A' && ch <= 'Z') {
                    ch += 32;
                }
                tailUrl = "/" + ch + methodName.substring(index + 1);
            }
            UrlParser parser = UrlParser.of(contentPath + tailUrl);

            Map<String, ConfigValue> pathVarMap = new HashMap<>();
            Map<String, ConfigValue> paramIndexMap = new HashMap<>();
            Map<String, ConfigValue> headersIndexMap = new HashMap<>();
            int entityIndex = -1;
            boolean bodyRequired = true;
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                for (Annotation annotation : parameter.getAnnotations()) {
                    if (annotation instanceof RequestBody) {
                        if (entityIndex == -1) {
                            entityIndex = i;
                            bodyRequired = parse(annotation, "required");
                        } else {
                            throw new IllegalArgumentException("RequestBody 只能存在一个");
                        }
                    } else {
                        String key = parse(annotation, "name");
                        if ("".equals(key)) {
                            key = parameter.getName();
                        }
                        ConfigValue configValue = new ConfigValue();
                        configValue.setIndex(i);
                        if (annotation instanceof PathVariable) {
                            configValue.setRequire(true);
                            pathVarMap.put(key, configValue);
                        } else {
                            String value = parse(annotation, "defaultValue");
                            boolean require = parse(annotation, "required");
                            configValue.setDefaultValue(value);
                            configValue.setRequire(require);
                            if (annotation instanceof RequestParam) {
                                paramIndexMap.put(key, configValue);
                            } else if (annotation instanceof RequestHeaders) {
                                headersIndexMap.put(key, configValue);
                            }
                        }
                    }
                }
            }
            int finalIndex = entityIndex;
            boolean finalBodyRequired = bodyRequired;

            BiFunction<Object[], ResponsePromise, Map<String, String>> paramsMapFunction = (args, promise) ->
                    Methods.getArgsByIndex(paramIndexMap, args, RequestParam.class, promise);
            BiFunction<Object[], ResponsePromise, Map<String, String>> headersMapFunction = (args, promise) ->
                    Methods.getArgsByIndex(headersIndexMap, args, RequestHeaders.class, promise);
            BiFunction<Object[], ResponsePromise, String[]> pathFunction = (args, promise) -> parser.parsePath(
                    Methods.getArgsByIndex(pathVarMap, args, PathVariable.class, promise));

            return (args, promise) -> {
                RequestArgs requestArgs = new RequestArgs();
                if (finalIndex != -1) {
                    Object entity = args[finalIndex];
                    if (entity != null || !finalBodyRequired) {
                        requestArgs.setEntity(JSON.toJSONString(args[finalIndex]));
                    } else {
                        promise.receive(
                                new IllegalArgumentException("entity can not be null"),
                                CauseType.REQUEST_NULL_HTTPBODY);
                    }
                }
                Host host = loadBalancer.select();
                requestArgs.setHost(host.getIp());
                requestArgs.setPort(host.getPort());
                requestArgs.setMethod(httpMethod);
                requestArgs.setPath(pathFunction.apply(args, promise));
                requestArgs.setParam(paramsMapFunction.apply(args, promise));
                requestArgs.setHeaders(headersMapFunction.apply(args, promise));
                requestArgs.setTimeout(timeout);
                return requestArgs;
            };
        }

        private ResponseDecoder getResponseDecoder(Method method) throws ClassNotFoundException {
            Class returnClass = method.getReturnType();
            Type genReturnType = method.getGenericReturnType();
            if (InvokeUtil.isFinalOrOptional(returnClass)) {
                genReturnType = InvokeUtil.getTypeArgument(genReturnType);
                returnClass = InvokeUtil.getClassByType(genReturnType);
            }

            final Class finalClass = returnClass;

            if (finalClass == void.class) {
                return str -> null;
            }

            if (finalClass == String.class) {
                return str -> str;
            }

            if (finalClass.isArray()) {
                Class clazz = finalClass.getComponentType();
                return str -> {
                    List list = JSON.parseArray(str, clazz);
                    Object[] objects = (Object[]) Array.newInstance(clazz, list.size());
                    list.toArray(objects);
                    return objects;
                };
            }

            if (InvokeUtil.isAssignable(Collection.class, finalClass)) {
                Function<String, List> listFunction;
                if (genReturnType instanceof ParameterizedType) {
                    ParameterizedType returnType = (ParameterizedType) genReturnType;
                    Type typeArg = returnType.getActualTypeArguments()[0];
                    Class clazz = Class.forName(typeArg.getTypeName());
                    listFunction = res -> JSON.parseArray(res, clazz);
                } else {
                    listFunction = JSON::parseArray;
                }
                if (InvokeUtil.isAssignable(ArrayList.class, finalClass)) {
                    return listFunction::apply;
                }
                if (InvokeUtil.isAssignable(LinkedList.class, finalClass)) {
                    return str -> new LinkedList<>(listFunction.apply(str));
                }
                if (InvokeUtil.isAssignable(HashSet.class, finalClass)) {
                    return str -> new HashSet<>(listFunction.apply(str));
                }
                if (InvokeUtil.isAssignable(TreeSet.class, finalClass)) {
                    return str -> new TreeSet<>(listFunction.apply(str));
                }

                for (Constructor cons : finalClass.getConstructors()) {
                    if (cons.getParameterCount() == 1) {
                        Class clazz = cons.getParameterTypes()[0];
                        if (clazz.isAssignableFrom(List.class)) {
                            return str -> cons.newInstance(listFunction.apply(str));
                        }
                    }
                }
            }
            return res -> JSON.parseObject(res, finalClass);
        }
    }

    class ConfigValue {
        private int index;
        private boolean require;
        private String defaultValue;

        int getIndex() {
            return this.index;
        }

        boolean isRequire() {
            return this.require;
        }

        String getDefaultValue() {
            return this.defaultValue;
        }

        void setIndex(int index) {
            this.index = index;
        }

        void setRequire(boolean require) {
            this.require = require;
        }

        void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}

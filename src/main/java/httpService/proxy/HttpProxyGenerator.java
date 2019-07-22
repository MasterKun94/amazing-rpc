package httpService.proxy;

import com.alibaba.fastjson.JSON;
import httpService.HttpMethod;
import httpService.RequestArgs;
import httpService.annotation.*;
import httpService.connectors.Connector;
import httpService.connectors.ConnectorBuilder;
import httpService.connectors.ConnectorType;
import httpService.connectors.netty.ResponsePromise;
import httpService.util.AliasUtil;
import httpService.util.UrlParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

/**
 * http请求代理生成器，
 */
@SuppressWarnings("unchecked")
public class HttpProxyGenerator<T> {
    private Connector connector;
    private String contentPath;
    private String host;
    private int port;
    private long timeout;
    private T proxy;
    private T fallBack;
    private Map<Method, FallBackMethod> fallBackMethodMap;//TODO

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
        if (clazz.isAnnotationPresent(ServiceContext.class)) {
            ServiceContext serviceContext = clazz.getAnnotation(ServiceContext.class);
            this.contentPath = AliasUtil.parse(serviceContext, "path");
            this.host = AliasUtil.parse(serviceContext, "host");
            this.port = AliasUtil.parse(serviceContext, "port");
            this.timeout = AliasUtil.parse(serviceContext, "timeout");

            capacity = AliasUtil.parse(serviceContext, "poolCapacity");
            lazy = AliasUtil.parse(serviceContext, "lazyInit");
            showRequest = AliasUtil.parse(serviceContext, "showRequest");
            showResponse = AliasUtil.parse(serviceContext, "showResponse");
            type = AliasUtil.parse(serviceContext, "connector");
            RequestHeaders[] heads = AliasUtil.parse(serviceContext, "defaultHeaders");
            defaultHeaders = new HashMap<>(heads.length);
            for (RequestHeaders head : heads) {
                String name = AliasUtil.parse(head, "name");
                String value = AliasUtil.parse(head, "defaultValue");
                defaultHeaders.put(name, value);
            }
        } else {
            throw new IllegalArgumentException("Annotation ServiceContext not found");
        }

        if (type == ConnectorType.NETTY) {
            this.connector = ConnectorBuilder
                    .createNetty(host, port)
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
            ResponsePromise promise = ResponsePromise.create(method.getReturnType());
            RequestEncoder encoder = getRequestEncoder(method);
            ResponseDecoder decoder = getResponseDecoder(method);
            ArgsToPromise arg2Obj = Methods.argsToPromise(promise, encoder, connector, decoder); //TODO
            FallBackMethod fallBackMethod = Methods.createFallBack(method, fallBack);
            return Methods.asyncProxyMethod(arg2Obj, fallBackMethod, method, timeout);
        }

        private RequestEncoder getRequestEncoder(Method method) {
            String tailUrl;
            HttpMethod httpMethod;
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                httpMethod = requestMapping.method();
                tailUrl = AliasUtil.parse(requestMapping, "path");
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

            Map<String, ConfigVaule> pathVarMap = new HashMap<>();
            Map<String, ConfigVaule> paramIndexMap = new HashMap<>();
            Map<String, ConfigVaule> headersIndexMap = new HashMap<>();
            int entityIndex = -1;
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                for (Annotation annotation : parameter.getAnnotations()) {
                    if (annotation instanceof RequestBody) {
                        if (entityIndex == -1) {
                            entityIndex = i;
                        } else {
                            throw new IllegalArgumentException("RequestBody 只能存在一个");
                        }
                    } else {
                        String key = AliasUtil.parse(annotation, "name");
                        if ("".equals(key)) {
                            key = parameter.getName();
                        }
                        ConfigVaule configVaule = new ConfigVaule();
                        configVaule.setIndex(i);
                        if (annotation instanceof PathVariable) {
                            configVaule.setRequire(true);
                            pathVarMap.put(key, configVaule);
                        } else {
                            String value = AliasUtil.parse(annotation, "defaultValue");
                            boolean require = AliasUtil.parse(annotation, "required");
                            configVaule.setDefaultValue(value);
                            configVaule.setRequire(require);
                            if (annotation instanceof RequestParam) {
                                paramIndexMap.put(key, configVaule);
                            } else if (annotation instanceof RequestHeaders) {
                                headersIndexMap.put(key, configVaule);
                            }
                        }
                    }
                }
            }
            int finalIndex = entityIndex;

            Function<Object[], Map<String, String>> paramsMapFunction = args ->
                    Methods.getArgsByIndex(paramIndexMap, args, RequestParam.class);
            Function<Object[], Map<String, String>> headersMapFunction = args ->
                    Methods.getArgsByIndex(headersIndexMap, args, RequestHeaders.class);
            Function<Object[], String[]> pathFunction = args -> parser.parsePath(
                    Methods.getArgsByIndex(pathVarMap, args, PathVariable.class));

            return args -> {
                RequestArgs requestArgs = new RequestArgs();
                if (finalIndex != -1) {
                    requestArgs.setEntity(JSON.toJSONString(args[finalIndex]));
                }
                requestArgs.setHost(host);
                requestArgs.setPort(port);
                requestArgs.setMethod(httpMethod);
                requestArgs.setPath(pathFunction.apply(args));
                requestArgs.setParam(paramsMapFunction.apply(args));
                requestArgs.setHeaders(headersMapFunction.apply(args));
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

    class ConfigVaule {
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

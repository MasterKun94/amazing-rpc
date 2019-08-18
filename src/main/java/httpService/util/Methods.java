package httpService.util;

import com.alibaba.fastjson.JSON;
import httpService.annotation.*;
import httpService.connection.RpcExecutor;
import httpService.exceptions.BadRequestException;
import httpService.exceptions.CauseType;
import httpService.exceptions.UnexpectedException;
import httpService.util.fallBack.FallBackInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

import static httpService.util.AliasUtil.parse;

@SuppressWarnings("unchecked")
public class Methods<T> {
    private Method method;
    private Decoder<T> decoder;
    private RpcExecutor rpcExecutor;
    private ServiceConfig config;
    private LoadBalancer balancer;
    private Object fallBackObject;

    static Methods of(Method method) throws ClassNotFoundException {
        Methods methods = new Methods();
        methods.method = method;
        methods.decoder = decode(method.getGenericReturnType());

        return methods;
    }

    Methods setFallBack(Object fallBack) {
        this.fallBackObject = fallBack;
        return this;
    }

    Methods setConfig(ServiceConfig config) {
        this.config = config;
        return this;
    }

    Methods setBalancer(LoadBalancer balancer) {
        this.balancer = balancer;
        return this;
    }

    Methods setRpcExecutor(RpcExecutor rpcExecutor) {
        this.rpcExecutor = rpcExecutor;
        return this;
    }

    ProxyMethod build() {
        Encoder encoder = encode(method, config.getContextPath(), balancer);
        ArgsToPromise<T> argsToPromise = argsToPromise(encoder, rpcExecutor, decoder);
        return asyncProxyMethod(argsToPromise, config.getTimeout(), method);
    }

    private static String[][] getArgsByIndex(
            Map<String, ? extends ConfigValue> paramIndices,
            Object[] args,
            Class clazz,
            ResponsePromise promise) {

        if (paramIndices.isEmpty() || promise.isDone()) {
            return null;
        } else {
            String[][] params = new String[paramIndices.size()][2];
            Set<String> keySet = paramIndices.keySet();
            int i = 0;
            for (String s : keySet) {
                ConfigValue configValue = paramIndices.get(s);

                String arg = (String) args[configValue.getIndex()];
                if (arg == null) {
                    if (configValue.getDefaultValue() != null) {
                        arg = configValue.getDefaultValue();
                    } else if (configValue.isRequire()) {
                        CauseType causeType;
                        if (clazz.equals(RequestHeaders.class)) {
                            causeType = CauseType.REQUEST_NULL_HEADERS;
                        } else if (clazz.equals(RequestParam.class)) {
                            causeType = CauseType.REQUEST_NULL_PARAM;
                        } else if (clazz.equals(PathVariable.class)) {
                            causeType = CauseType.REQUEST_NULL_PATHVAR;
                        } else {
                            causeType = CauseType.DEFAULT;
                        }
                        promise.receive(new BadRequestException("Annotation " +
                                clazz + " value not available"), causeType);
                    }
                }

                params[i][0] = s;
                params[i][1] = arg;
                i++;
            }
            return params;
        }
    }

    private static Encoder encode(Method method, String contentPath, LoadBalancer balancer) {
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
        Map<String, ConfigValue> paramsMap = new HashMap<>();
        Map<String, ConfigValue> headersMap = new HashMap<>();
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
                        System.out.println(parser);
                        String index = parser.getIndex(key);
                        pathVarMap.put(index, configValue);
                    } else {
                        String value = parse(annotation, "defaultValue");
                        boolean require = parse(annotation, "required");
                        configValue.setDefaultValue(value);
                        configValue.setRequire(require);
                        if (annotation instanceof RequestParam) {
                            paramsMap.put(key, configValue);
                        } else if (annotation instanceof RequestHeaders) {
                            headersMap.put(key, configValue);
                        }
                    }
                }
            }
        }
        int finalIndex = entityIndex;
        boolean finalBodyRequired = bodyRequired;

        BiFunction<Object[], ResponsePromise, String[][]> paramsFunc = (args, promise) ->
                Methods.getArgsByIndex(paramsMap, args, RequestParam.class, promise);
        BiFunction<Object[], ResponsePromise, String[][]> headersFunc = (args, promise) ->
                Methods.getArgsByIndex(headersMap, args, RequestHeaders.class, promise);
        BiFunction<Object[], ResponsePromise, StringBuilder> pathFunction = (args, promise) -> parser.parsePath(
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
                            CauseType.REQUEST_NULL_BODY);
                }
            }
            requestArgs.setAddress(balancer);
            requestArgs.setMethod(httpMethod);
            requestArgs.setPath(pathFunction.apply(args, promise));
            requestArgs.setParams(paramsFunc.apply(args, promise));
            requestArgs.setHeaders(headersFunc.apply(args, promise));
            return requestArgs;
        };
    }

    public static Decoder decode(Type returnType) throws ClassNotFoundException {//TODO
        Class returnClass = getClassByType(returnType);
        if (isFinalOrOptional(returnClass)) {
            returnType = getTypeArgument(returnType);
            returnClass = getClassByType(returnType);
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

        if (isAssignable(Collection.class, finalClass)) {
            Function<String, List> listFunction;
            if (returnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) returnType;
                Type typeArg = parameterizedType.getActualTypeArguments()[0];
                Class clazz = Class.forName(typeArg.getTypeName());
                listFunction = res -> JSON.parseArray(res, clazz);
            } else {
                listFunction = JSON::parseArray;
            }
            if (isAssignable(ArrayList.class, finalClass)) {
                return listFunction::apply;
            }
            if (isAssignable(LinkedList.class, finalClass)) {
                return str -> new LinkedList<>(listFunction.apply(str));
            }
            if (isAssignable(HashSet.class, finalClass)) {
                return str -> new HashSet<>(listFunction.apply(str));
            }
            if (isAssignable(TreeSet.class, finalClass)) {
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

    private ProxyMethod asyncProxyMethod(
            ArgsToPromise<T> arg2Prms,
            long timeout,
            Method method) {

        Class returnType = method.getReturnType();
        ProxyMethod finalArg2Obj;

        if (ResponseFuture.class.isAssignableFrom(returnType)) {
            return arg2Prms::apply;
        }

        if (Future.class.isAssignableFrom(returnType)) {

            return args -> {
                ClientResponsePromise<T> promise = new ClientResponsePromise<>();

                arg2Prms.apply(args).addListener(f -> {
                    if (f.isDoneAndSuccess()) {
                        promise.receive(f.getEntity());
                    } else {
                        try {
                            FallBackInfo.setCause(f.getCause());
                            FallBackInfo.setCauseType(f.getCauseType());

                            promise.receive(((Future<T>) method.invoke(this.fallBackObject, args)).get());
                        } catch (Exception e) {
                            throw new UnexpectedException(e);

                        } finally {
                            FallBackInfo.reset();
                        }
                    }
                });
                return promise;
            };
        }

        Function<Object, Object> function;
        if (Optional.class.isAssignableFrom(returnType)) {
            function = Optional::ofNullable;
        } else {
            function = object -> object;
        }

        return args -> {//TODO
            ResponseFuture<T> future = arg2Prms.apply(args);
            if (future.whenSuccess(timeout)) {
                return function.apply(future.getEntity());
            } else {
                try {
//                    future.getCause().printStackTrace();
                    FallBackInfo.setCause(future.getCause());
                    FallBackInfo.setCauseType(future.getCauseType());
                    return method.invoke(this.fallBackObject, args);
                } finally {
                    FallBackInfo.reset();
                }

            }
        };
    }


    private static <T> ArgsToPromise<T> argsToPromise(
            Encoder encoder,
            RpcExecutor rpcExecutor,
            Decoder<T> decoder) {

        return (args) -> {
            ResponsePromise<T> promise = ResponsePromise.create();
            RequestArgs requestArgs = encoder.encode(args, promise);
            return promise.isDone() ?
                    promise :
                    rpcExecutor.executeAsync(requestArgs, decoder, promise);
        };
    }


    private static boolean isFinalOrOptional(Class clazz) {
        return Future.class.isAssignableFrom(clazz) ||
                Optional.class.isAssignableFrom(clazz);
    }

    private static Type getTypeArgument(Type type) {
        ParameterizedType returnType = (ParameterizedType) type;
        return returnType.getActualTypeArguments()[0];
    }

    private static Class getClassByType(Type type) {
        String typeName = type.getTypeName().split("<")[0];
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> boolean isAssignable(Class<T> parent, Class child) {
        return parent.isAssignableFrom(child);
    }

    static class ConfigValue {
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

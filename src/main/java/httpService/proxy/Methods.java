package httpService.proxy;

import com.alibaba.fastjson.JSON;
import httpService.HttpMethod;
import httpService.RequestArgs;
import httpService.annotation.*;
import httpService.connectors.Connector;
import httpService.connectors.LoadBalancer;
import httpService.connectors.netty.ResponsePromise;
import httpService.exceptions.BadRequestException;
import httpService.exceptions.CauseType;
import httpService.util.UrlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

import static httpService.util.AliasUtil.parse;

class Methods {
    private static final Logger logger = LoggerFactory.getLogger(Methods.class);

    private static Map<String, String> getArgsByIndex(
            Map<String, ? extends ConfigValue> paramIndices,
            Object[] args,
            Class clazz,
            ResponsePromise promise) {

        if (paramIndices.isEmpty() || promise.isDone()) {
            return null;
        } else {
            Map<String, String> paramMap = new HashMap<>(paramIndices.size());
            for (String s : paramIndices.keySet()) {
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
                paramMap.put(s, arg);
            }
            return paramMap;
        }
    }

    public static RequestEncoder encode(Method method, String contentPath, LoadBalancer loadBalancer, long timeout) {
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
                        pathVarMap.put(key, configValue);
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

        BiFunction<Object[], ResponsePromise, Map<String, String>> paramsFunc = (args, promise) ->
                Methods.getArgsByIndex(paramsMap, args, RequestParam.class, promise);
        BiFunction<Object[], ResponsePromise, Map<String, String>> headersFunc = (args, promise) ->
                Methods.getArgsByIndex(headersMap, args, RequestHeaders.class, promise);
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
                            CauseType.REQUEST_NULL_BODY);
                }
            }
            requestArgs.setLoadBalancer(loadBalancer);
            requestArgs.setMethod(httpMethod);
            requestArgs.setPath(pathFunction.apply(args, promise));
            requestArgs.setParam(paramsFunc.apply(args, promise));
            requestArgs.setHeaders(headersFunc.apply(args, promise));
            requestArgs.setTimeout(timeout);
            return requestArgs;
        };
    }

    @SuppressWarnings("unchecked")
    static ResponseDecoder decode(Method method) throws ClassNotFoundException {
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

    static <T> ProxyMethod asyncProxyMethod(
            ArgsToPromise<T> arg2Prms,
            long timeout,
            Method method) {
        Class returnType = method.getReturnType();
        ProxyMethod finalArg2Obj;
        if (Future.class.isAssignableFrom(returnType)) {
            return arg2Prms::apply;
        }
        ProxyMethod proxyMethod = args -> {//TODO
            ResponsePromise<T> promise = arg2Prms.apply(args);
            promise.whenSuccess(timeout);
            return promise.getEntity();
        };
        if (Optional.class.isAssignableFrom(returnType)) {
            finalArg2Obj = args -> Optional.ofNullable(proxyMethod.apply(args));
        } else {
            finalArg2Obj = proxyMethod;
        }
        return finalArg2Obj;
    }


    static <T> ArgsToPromise<T> argsToPromise(
            RequestEncoder encoder,
            Connector connector,
            ResponseDecoder<T> decoder) {

        return (args) -> {
            ResponsePromise<T> promise = ResponsePromise.create();
            RequestArgs requestArgs = encoder.encode(args, promise);
            return promise.isDone() ?
                    promise :
                    connector.executeAsync(requestArgs, decoder, promise);
        };
    }

}

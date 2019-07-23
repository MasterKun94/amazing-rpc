package httpService.proxy;

import httpService.RequestArgs;
import httpService.annotation.PathVariable;
import httpService.annotation.RequestHeaders;
import httpService.annotation.RequestParam;
import httpService.connectors.Connector;
import httpService.connectors.netty.ResponseFuture;
import httpService.connectors.netty.ResponsePromise;
import httpService.exceptions.CauseType;
import httpService.exceptions.UnexpectedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

class Methods {

    static <T> FallBackMethod createFallBack(Method method, T fallBackObject) {//TODO
        return fallBackObject == null ?
                new DefaultFallBackMethod() :
                (args, e, type) -> {
                    try {
                        return method.invoke(fallBackObject, args);
                    } catch (IllegalAccessException | InvocationTargetException e1) {
                        throw new UnexpectedException();
                    }
                };
    }

    @Deprecated
    static <T> ArgsToObject argsToObject(
            ResponsePromise<T> promise,
            RequestEncoder encoder,
            Connector connector,
            ResponseDecoder<T> decoder,
            long timeout) {
        ArgsToPromise<T> argsToPromise = argsToPromise(promise, encoder, connector, decoder);
        return args -> {
            ResponsePromise<T> responsePromise = argsToPromise.apply(args);
            if (responsePromise.whenSuccess(timeout)) {
                return responsePromise.getEntity();
            } else {
                throw responsePromise.getCause();
            }
        };
    }

    static <T> ArgsToPromise<T> argsToPromise(
            ResponsePromise<T> promise,
            RequestEncoder encoder,
            Connector connector,
            ResponseDecoder<T> decoder) {

        return args -> {
            RequestArgs requestArgs = encoder.encode(args, promise);
            return promise.isDoneAndFailed() ?
                    promise :
                    connector.executeAsync(requestArgs, promise, decoder);
        };
    }

    @Deprecated
    static ProxyMethod proxyMethod(
            ArgsToObject arg2Obj,
            FallBackMethod fallBackMethod,
            Method method) {

        ProxyMethod proxyMethod = args -> {
            try {
                return arg2Obj.apply(args);
            } catch (Throwable e) {
                return fallBackMethod.apply(args, e, null);
            }
        };
        Class returnType = method.getReturnType();

        if (Future.class.isAssignableFrom(returnType)) {
            return args -> CompletableFuture.supplyAsync(() -> proxyMethod.apply(args));
        } else if (Optional.class.isAssignableFrom(returnType)) {
            return args -> Optional.ofNullable(proxyMethod.apply(args));
        } else {
            return proxyMethod;
        }
    }

    static <T> ProxyMethod asyncProxyMethod(
            ArgsToPromise<T> arg2Prms,
            FallBackMethod fallBack,
            Method method,
            long timeout) {

        Class returnType = method.getReturnType();
        ProxyMethod finalArg2Obj;
        if (ResponseFuture.class.isAssignableFrom(returnType)) {
            return arg2Prms::apply;
        }
        ProxyMethod proxyMethod = args -> {
            ResponsePromise<T> promise = arg2Prms.apply(args);
            if (promise.whenSuccess(timeout)){
                return promise.getEntity();
            } else{
                Throwable cause = promise.getCause();
                CauseType type = promise.getCauseType();
                return fallBack.apply(args, cause, type);
            }
        };
        if (Future.class.isAssignableFrom(returnType)) {
            finalArg2Obj = args -> CompletableFuture.supplyAsync(() -> proxyMethod.apply(args));
        } else if (Optional.class.isAssignableFrom(returnType)) {
            finalArg2Obj = args -> Optional.ofNullable(proxyMethod.apply(args));
        } else {
            finalArg2Obj = proxyMethod;
        }
        return finalArg2Obj;
    }

    static Map<String, String> getArgsByIndex(
            Map<String, ? extends HttpProxyGenerator.ConfigValue> paramIndices,
            Object[] args,
            Class clazz,
            ResponsePromise promise) {

        if (paramIndices.isEmpty()) {
            return null;
        } else {
            Map<String, String> paramMap = new HashMap<>(paramIndices.size());
            for (String s : paramIndices.keySet()) {
                HttpProxyGenerator.ConfigValue configValue = paramIndices.get(s);

                String arg = (String) args[configValue.getIndex()];
                if (arg == null) {
                    if (configValue.getDefaultValue() != null) {
                        arg = configValue.getDefaultValue();
                    } else if (configValue.isRequire()) {
                        CauseType causeType;
                        if (clazz.equals(RequestHeaders.class)) {
                            causeType = CauseType.REQUEST_NULL_HTTPHEADERS;
                        } else if (clazz.equals(RequestParam.class)) {
                            causeType = CauseType.REQUEST_NULL_HTTPPARAM;
                        } else if (clazz.equals(PathVariable.class)) {
                            causeType = CauseType.REQUEST_NULL_PATHVARIABLE;
                        } else {
                            causeType = CauseType.DEFAULT;
                        }
                        promise.receive(new IllegalArgumentException("Annotation " +
                                clazz + " value not available"), causeType);
                    }
                }
                paramMap.put(s, arg);
            }
            return paramMap;
        }
    }
}

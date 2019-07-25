package httpService.proxy;

import httpService.RequestArgs;
import httpService.annotation.PathVariable;
import httpService.annotation.RequestHeaders;
import httpService.annotation.RequestParam;
import httpService.connectors.Connector;
import httpService.connectors.netty.FallBackMethod;
import httpService.connectors.netty.ResponsePromise;
import httpService.exceptions.CauseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

class Methods {
    private static final Logger logger = LoggerFactory.getLogger(Methods.class);

    @Deprecated
    static <T> ArgsToObject argsToObject(
            RequestEncoder encoder,
            Connector connector,
            ResponseDecoder<T> decoder,
            long timeout) {
        ArgsToPromise<T> argsToPromise = argsToPromise(encoder, connector, decoder);
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

    @Deprecated
    static ProxyMethod proxyMethod(
            ArgsToObject arg2Obj,
            FallBackMethod fallBackMethod,
            Method method) {

        ProxyMethod proxyMethod = args -> {
            try {
                return arg2Obj.apply(args);
            } catch (Throwable e) {
                return fallBackMethod.apply(e, null);
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
            long timeout,
            Class<T> returnType) {
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

package httpService.annotation;

import httpService.connectors.ConnectorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来定义识该注解的类的所有请求url的前缀部分，可以用来定义域名，ip地址和端口号等
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceContext {
    String host();

    int port();

    String path() default "";

    long timeout() default 15000L;

    int poolCapacity() default 32;

    boolean lazyInit() default false;

    boolean showRequest() default false;

    boolean showResponse() default false;

    ConnectorType connector() default ConnectorType.NETTY;

    RequestHeaders[] defaultHeaders() default {
            @RequestHeaders(name = "Accept", defaultValue = "application/json"),
            @RequestHeaders(name = "Content-Type", defaultValue = "application/json"),
            @RequestHeaders(name = "Connection", defaultValue = "keep-alive"),
            @RequestHeaders(name = "Cache-Control", defaultValue = "no-cache")
    };
}

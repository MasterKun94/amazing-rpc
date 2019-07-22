package httpService.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识该注解的方法参数会被绑定到http的请求体中，请求默认为 restful 风格，方法参数
 * 首先会通过 {@link com.alibaba.fastjson.JSON} 序列化成json字符串并写入请求体，
 * 再会添加头部信息 Content-type: application/json, Accept: application/json
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {

    boolean required() default true;
}

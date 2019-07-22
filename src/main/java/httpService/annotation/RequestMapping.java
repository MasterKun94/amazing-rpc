package httpService.annotation;

import httpService.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来定义识该注解的类的所有请求url的后缀部分以及请求类型
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {

    /**
     * 定义请求url的后部分，可以写成url模板，并在方法参数中添加
     * {@code @PathVariable}
     *
     */
    @Alias("path")
    String value() default "";

    String path() default "";

    /**
     * 请求类型，有 GET, POST, PUT, DELETE 四个可选项
     */
    HttpMethod method() default HttpMethod.GET;
}

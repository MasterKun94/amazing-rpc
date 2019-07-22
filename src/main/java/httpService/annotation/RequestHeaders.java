package httpService.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识该注解的方法参数会被绑定到http请求的头部信息中
 *
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHeaders {

    /**
     * 定义http头部信息中的key，当{@code value()}不为默认值时，key为对应的值，当
     * {@code value()}为默认值时，key为方法中对应的参数名称，但是请注意使用默认值时，
     * 请确保添加 javac 的编译参数 -parameters ，不然会识别不了参数的名称。
     *
     */
    @Alias("name")
    String value() default "";

    String name() default "";

    String defaultValue() default "";

    boolean required() default true;
}

package httpService.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识该注解的方法参数会绑定到 url 模板上，模板内的参数用大括号标识以下是存在
 * 一个路径变量名为 pathVar 的 url 模板示例：
 * http://example/{pathVar}/1234
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {

    /**
     * 当{@code value()} 不为默认值时，变量名为对应的值，当 {@code value()}
     * 为默认值时，变量名为方法中对应的参数名称，但是请注意使用默认值时，请确保添
     * 加 javac 的编译参数 -parameters ，不然会识别不了参数的名称。
     *
     * @return url 中的变量名
     */
    @Alias("name")
    String value() default "";

    String name() default "";
}

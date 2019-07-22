package httpService.util;

import httpService.annotation.Alias;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AliasUtil {

    @SuppressWarnings("unchecked")
    public static <T> T parse(Annotation annotation, String method) {
        try {
            Class<? extends Annotation> clazz = annotation.annotationType();
            Method requestMethod = clazz.getMethod(method);
            Object defaultValue = requestMethod.getDefaultValue();
            Object anotherValue = requestMethod.invoke(annotation);
            if (!anotherValue.equals(defaultValue)) {
                return (T) anotherValue;
            }
            for (Method aliasMethod : clazz.getMethods()) {
                if (aliasMethod.isAnnotationPresent(Alias.class)) {
                    Alias alias = aliasMethod.getAnnotation(Alias.class);
                    if (alias.value().equals(method)) {
                        anotherValue = aliasMethod.invoke(annotation);
                        if (!anotherValue.equals(defaultValue) &&
                            !anotherValue.equals(aliasMethod.getDefaultValue())) {
                            return (T) anotherValue;
                        }
                    }
                }
            }
            return (T) defaultValue;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

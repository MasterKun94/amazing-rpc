package httpService.proxy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.Future;

public class InvokeUtil {

    static boolean isFinalOrOptional(Class clazz) {
        return Future.class.isAssignableFrom(clazz) ||
                Optional.class.isAssignableFrom(clazz);
    }

    static Type getTypeArgument(Type type) {
        ParameterizedType returnType = (ParameterizedType) type;
        return returnType.getActualTypeArguments()[0];
    }

    static Class getClassByType(Type type) {
        String typeName = type.getTypeName().split("<")[0];
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static <T> boolean isAssignable(Class<T> parent, Class child) {
        return parent.isAssignableFrom(child);
    }
}

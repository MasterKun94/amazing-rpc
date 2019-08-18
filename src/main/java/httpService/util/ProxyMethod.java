package httpService.util;

import java.lang.reflect.InvocationTargetException;

public interface ProxyMethod {

    Object apply(Object[] args) throws InvocationTargetException, IllegalAccessException;
}

package httpService.proxy;

import httpService.exceptions.CauseType;

public interface FallBackMethod<T> {
    T apply(Object[] args, Throwable e, CauseType type);
}

package httpService.connectors.netty;

import httpService.exceptions.CauseType;

public interface FallBackMethod<T> {
    T apply(Throwable e, CauseType type);
}

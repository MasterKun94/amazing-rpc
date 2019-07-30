package httpService.proxy;

import httpService.exceptions.CauseType;

public interface FallBackMethod {
    <T> T apply(Throwable e, CauseType type);
}

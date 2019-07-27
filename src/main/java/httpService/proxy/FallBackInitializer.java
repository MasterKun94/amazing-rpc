package httpService.proxy;

import httpService.exceptions.CauseType;

public interface FallBackInitializer<T> {

    T init(Throwable cause, CauseType causeType);
}

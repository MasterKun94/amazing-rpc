package httpService.util;

import httpService.exceptions.CauseType;

public interface FallBackMethod {
    <T> T apply(Throwable e, CauseType type);
}

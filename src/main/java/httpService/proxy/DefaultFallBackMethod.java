package httpService.proxy;

import httpService.exceptions.CauseType;
import httpService.exceptions.UnhandledException;

public class DefaultFallBackMethod implements FallBackMethod {

    @Override
    public <T> T apply(Throwable e, CauseType type) {
        throw new UnhandledException(e, type);
    }
}

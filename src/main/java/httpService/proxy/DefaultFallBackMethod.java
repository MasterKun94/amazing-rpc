package httpService.proxy;

import httpService.exceptions.CauseType;

public class DefaultFallBackMethod implements FallBackMethod {

    @Override
    public Object apply(Object[] args, Throwable e, CauseType type) {
        throw new RuntimeException(e);//TODO
    }
}

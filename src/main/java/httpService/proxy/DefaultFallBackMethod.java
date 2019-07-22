package httpService.proxy;

import httpService.exceptions.CauseType;
import httpService.exceptions.UnhandledException;

public class DefaultFallBackMethod implements FallBackMethod {

    @Override
    public Object apply(Object[] args, Throwable e, CauseType type) {
        if (
                type == CauseType.REQUEST_NULL_HTTPBODY ||
                type == CauseType.REQUEST_NULL_HTTPHEADERS ||
                type == CauseType.REQUEST_NULL_HTTPPARAM ||
                type == CauseType.REQUEST_NULL_PATHVARIABLE ||
                type == CauseType.RESPONSE_DECODE_FAILED ||
                type == CauseType.RESPONSE_INVALID
        ) {
            throw new IllegalArgumentException(e);
        } else {
            throw new UnhandledException(e);//TODO
        }
    }
}

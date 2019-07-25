package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import httpService.exceptions.UnhandledException;

public class FinalFallBackMethod implements FallBackMethod {

    @Override
    public Object apply(Throwable e, CauseType type) {
        if (
                type == CauseType.REQUEST_NULL_HTTPBODY ||
                type == CauseType.REQUEST_NULL_HTTPHEADERS ||
                type == CauseType.REQUEST_NULL_HTTPPARAM ||
                type == CauseType.REQUEST_NULL_PATHVARIABLE ||
                type == CauseType.RESPONSE_DECODE_FAILED ||
                type == CauseType.RESPONSE_INVALID
        ) {
            throw new IllegalArgumentException(type.toString(), e);
        } else {
            throw new UnhandledException(e, type);//TODO
        }
    }
}

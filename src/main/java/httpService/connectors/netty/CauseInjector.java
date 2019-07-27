package httpService.connectors.netty;

import httpService.exceptions.CauseType;

public interface CauseInjector {
    void setCause(Throwable throwable);

    void setCauseType(CauseType causeType);
}

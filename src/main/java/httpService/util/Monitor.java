package httpService.util;

import httpService.exceptions.CauseType;

import java.net.InetSocketAddress;

public interface Monitor {
    void sendRequest();

    void receiveResponse();

    void reconnect(InetSocketAddress socketAddress);

    void connectException(Throwable e, CauseType type);
}

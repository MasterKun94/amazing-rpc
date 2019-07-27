package httpService;

import httpService.proxy.SocketAddress;

import java.util.Map;

public class DefaultArgs {
    private SocketAddress address;
    private final Map<String, String> defaultHeaders;

    public DefaultArgs(SocketAddress address, Map<String, String> defaultHeaders) {
        this.address = address;
        this.defaultHeaders = defaultHeaders;
    }

    public String getIp() {
        return address.getHost();
    }

    public int getPort() {
        return address.getPort();
    }

    public String getAddress() {
        return address.get();
    }

    public Map<String, String> getHeaders() {
        return defaultHeaders;
    }
}

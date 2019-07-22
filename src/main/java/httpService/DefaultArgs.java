package httpService;

import java.util.Map;

public class DefaultArgs {
    private final String ip;
    private final int port;
    private final String address;
    private final Map<String, String> defaultHeaders;

    public DefaultArgs(String ip, int port, Map<String, String> defaultHeaders) {
        this.ip = ip;
        this.port = port;
        this.address = ip + ":" + port;
        this.defaultHeaders = defaultHeaders;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    public Map<String, String> getHeaders() {
        return defaultHeaders;
    }
}

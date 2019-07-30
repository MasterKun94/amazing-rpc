package httpService;

import java.net.InetSocketAddress;

public class DefaultArgs {
    private InetSocketAddress address;
    private final String[][] defaultHeaders;

    public DefaultArgs(InetSocketAddress address, String[][] defaultHeaders) {
        this.address = address;
        this.defaultHeaders = defaultHeaders;
    }

    public String getHost() {
        return address.getHostName();
    }

    public int getPort() {
        return address.getPort();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String[][] getHeaders() {
        return defaultHeaders;
    }
}

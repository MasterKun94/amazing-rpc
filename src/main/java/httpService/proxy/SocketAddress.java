package httpService.proxy;

import java.util.List;

public class SocketAddress {
    private String host;
    private int port;
    private String address;

    SocketAddress(String host, int port) {
        this.host = host;
        this.port = port;
        this.address = host + ":" + port;
    }

    public String get() {
        return address;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public static List<SocketAddress> parse(String host) {
        return null;
    }
}

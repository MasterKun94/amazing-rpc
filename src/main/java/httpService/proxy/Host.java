package httpService.proxy;

import java.util.List;

public class Host {
    private String ip;
    private int port;
    private String host;

    Host(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.host = ip + ":" + port;
    }

    public String getHost() {
        return host;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public static List<Host> parse(String host) {
        return null;
    }
}

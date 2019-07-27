package httpService.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultServiceParser implements ServiceParser {
    public List<SocketAddress> parse(String service) {
        String trimService = service.trim();
        if (trimService.startsWith("http://")) {
            trimService = trimService.substring("http://".length());
        }
        String[] hosts = service.split(",");
        if (hosts.length == 1) {
            String[] addr = hosts[0].split(":");
            if (addr.length == 2) {
                SocketAddress socketAddress = new SocketAddress(addr[0], Integer.valueOf(addr[1]));
                return Collections.singletonList(socketAddress);
            }
        } else {
            List<SocketAddress> socketAddressList = new ArrayList<>();
            for (String str : hosts) {
                String[] addr = str.split(":");
                if (addr.length == 2) {
                    socketAddressList.add(new SocketAddress(addr[0], Integer.valueOf(addr[1])));
                } else {
                    throw new IllegalArgumentException("unable to resolve host: " + str);
                }
            }
            return socketAddressList;
        }
        return null;
    }
}

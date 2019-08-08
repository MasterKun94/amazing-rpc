package httpService.util;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultServiceParser implements ServiceParser {
    public List<InetSocketAddress> parse(String service) {
        String trimService = service.trim();
        if (trimService.startsWith("http://")) {
            trimService = trimService.substring("http://".length());
        }
        String[] hosts = trimService.split(",");
        InetSocketAddress address;
        if (hosts.length == 1) {
            String[] addr = hosts[0].split(":");
            if (addr.length == 2) {
                return Collections.singletonList(getAddress(addr));
            } else {
                return null;
            }
        } else {
            List<InetSocketAddress> socketAddressList = new ArrayList<>();
            for (String str : hosts) {
                String[] addr = str.split(":");
                if (addr.length == 2) {
                    socketAddressList.add(getAddress(addr));
                } else {
                    throw new IllegalArgumentException("unable to resolve host: " + str);
                }
            }
            return socketAddressList;
        }
    }

    private InetSocketAddress getAddress(String[] addr) {
        return new InetSocketAddress(addr[0], Integer.valueOf(addr[1]));
    }
}

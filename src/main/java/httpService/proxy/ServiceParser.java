package httpService.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceParser {
    static List<Host> parse(String service) {
        String trimService = service.trim();
        if (trimService.startsWith("http://")) {
            trimService = trimService.substring("http://".length());
        }
        String[] hosts = service.split(",");
        if (hosts.length == 1) {
            String[] addr = hosts[0].split(":");
            if (addr.length == 2) {
                Host host = new Host(addr[0], Integer.valueOf(addr[1]));
                return Collections.singletonList(host);
            } else {
                return parseService(service);
            }
        } else {
            List<Host> hostList = new ArrayList<>();
            for (String str : hosts) {
                String[] addr = str.split(":");
                if (addr.length == 2) {
                    hostList.add(new Host(addr[0], Integer.valueOf(addr[1])));
                } else {
                    throw new IllegalArgumentException("unable to resolve host: " + str);
                }
            }
            return hostList;
        }
    }

    private static List<Host> parseService(String service) {
        throw new UnsupportedOperationException();//TODO
    }
}

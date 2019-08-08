package httpService.util;

import java.net.InetSocketAddress;
import java.util.List;

public interface ServiceParser {
    List<InetSocketAddress> parse(String serviceName);

}

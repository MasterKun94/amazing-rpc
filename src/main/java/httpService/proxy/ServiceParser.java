package httpService.proxy;

import java.util.List;

public interface ServiceParser {
    List<SocketAddress> parse(String serviceName);

}

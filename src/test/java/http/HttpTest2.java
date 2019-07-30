package http;

import httpService.annotation.ServiceContext;

@ServiceContext(host = "www.baidu.com", showRequest = true, showResponse = true)
public interface HttpTest2 {

    String get();
}

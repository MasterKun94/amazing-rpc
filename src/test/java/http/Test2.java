package http;

import httpService.proxy.HttpProxyGenerator;
import httpService.proxy.HttpProxyGeneratorImpl;

public class Test2 {
    public static void main(String[] args) {
        HttpTest2 httptest2 = HttpProxyGenerator.start().getProxy(HttpTest2.class);
        httptest2.get();
    }
}

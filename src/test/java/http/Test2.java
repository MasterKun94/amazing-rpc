package http;

import httpService.util.HttpProxyGenerator;

public class Test2 {
    public static void main(String[] args) {
        HttpTest2 httptest2 = HttpProxyGenerator.start().getProxy(HttpTest2.class);
        httptest2.get();
    }
}

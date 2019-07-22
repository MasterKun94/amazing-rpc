package http;

import httpService.proxy.HttpProxyGenerator;

import java.util.Collections;

public class Test {

    public static void main(String[] args) throws Exception {
        HttpTest httpTest = HttpProxyGenerator.getProxy(HttpTest.class);
//        httpTest.get();
        for (int i = 0; i < 5; i++) {
            System.out.println(i + ", " + System.currentTimeMillis());
            System.out.println(httpTest.getMessage());
            System.out.println(httpTest.postMessage(Collections.singletonList(new Message("post"))).get());
            System.out.println(httpTest.putMessage(new Message("putTestMessage"), "testHead"));
            System.out.println(httpTest.deleteMessage("testMessage", "path123312").get());
            System.out.println(i + ", " + System.currentTimeMillis());
        }

    }
}

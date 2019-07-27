package http;

import httpService.proxy.HttpProxyGenerator;

import java.util.Collections;

public class Test {

    public static void main(String[] args) throws Exception {
        HttpTest httpTest = HttpProxyGenerator.start(HttpTest.class).getProxy();
//        httpTest.get();
        for (int i = 0; i < 5; i++) {
            System.out.println(i + ", " + System.currentTimeMillis());
            System.out.println(httpTest.getMessage());
//            Thread.sleep(2000);
            System.out.println(httpTest.postMessage(Collections.singletonList(new Message("post"))).get());
//            Thread.sleep(2000);
            System.out.println(httpTest.putMessage(new Message("putTestMessage"), "testHead"));
//            Thread.sleep(2000);
            System.out.println(httpTest.deleteMessage("testMessage", "path123312").get());
//            Thread.sleep(2000);
            System.out.println(i + ", " + System.currentTimeMillis());
        }

    }
}

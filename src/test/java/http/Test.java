package http;

import httpService.proxy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class Test {

    public static void main(String[] args) throws Exception {
        HttpTest httpTest = HttpProxyGenerator.start()
                .addInterceptor(new MonitorInitializer() {
                    @Override
                    public Monitor init() {
                        return new Monitor() {
                            private Logger logger = LoggerFactory.getLogger(this.getClass());

                            @Override
                            public void beforeSendRequest() {
                                logger.info("beforeSendRequest");
                            }

                            @Override
                            public void afterReceiveResponse(ResponseFuture future) {
                                logger.info("afterReceiveResponse");
                            }
                        };
                    }
                })
                .addInterceptor(new MonitorInitializer() {
                    @Override
                    public Monitor init() {
                        return new Monitor() {
                            private Logger logger = LoggerFactory.getLogger(this.getClass());

                            @Override
                            public void beforeSendRequest() {
                                logger.info("before0");
                            }

                            @Override
                            public void afterReceiveResponse(ResponseFuture future) {
                                logger.info("after0");
                            }
                        };
                    }
                })
                .getProxy(HttpTest.class);
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

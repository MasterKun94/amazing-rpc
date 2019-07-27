package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.ChannelHolder;

import java.util.concurrent.TimeUnit;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private static final EventLoopGroup group = new NioEventLoopGroup();

    public static Channel start(
            String host,
            int port,
            ResponsePromise promise,
            ChannelHolder holder,
            boolean showRequest,
            boolean showResponse) {
        Bootstrap bootstrap = new Bootstrap();
        ChannelFuture channelFuture = bootstrap
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new HttpPipelineInitializer(holder, showRequest, showResponse))
                .connect(host, port)
                .syncUninterruptibly();

        if (channelFuture.isSuccess())
            logger.debug("{}:{}, [{}] connect success", host, port, channelFuture.channel());
        else {
            if (promise != null) {
                promise.receive(channelFuture.cause(), CauseType.CONNECTION_CONNECT_FAILED);
            }
            logger.error("{}:{} connect fail", host, port);
            channelFuture.cause().printStackTrace();
        }

        return channelFuture.channel();
    }
}

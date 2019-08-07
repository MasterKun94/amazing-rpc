package httpService.connectors.netty;

import httpService.proxy.ReleaseAble;
import httpService.proxy.ResponsePromise;
import httpService.exceptions.CauseType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private static final EventLoopGroup GROUP = new NioEventLoopGroup();

    public static Channel start(
            InetSocketAddress address,
            ResponsePromise promise,
            ReleaseAble holder,
            SslContext sslContext,
            boolean showRequest,
            boolean showResponse) {

        Bootstrap bootstrap = new Bootstrap();
        ChannelFuture channelFuture = bootstrap
                .group(GROUP)
                .channel(NioSocketChannel.class)
                .handler(new HttpPipelineInitializer(holder, sslContext, showRequest, showResponse))
                .connect(address)
                .syncUninterruptibly();

        if (channelFuture.isSuccess()) {
            logger.debug("{}, [{}] connect success", address, channelFuture.channel());
        } else {
            if (promise != null) {
                promise.receive(channelFuture.cause(), CauseType.CONNECTION_CONNECT_FAILED);
            }
            logger.error("{} connect fail", address);
            channelFuture.cause().printStackTrace();
        }

        return channelFuture.channel();
    }
}

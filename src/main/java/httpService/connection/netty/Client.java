package httpService.connection.netty;

import httpService.util.ReleaseAble;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private static final EventLoopGroup GROUP = new NioEventLoopGroup();

    public static ChannelFuture start(
            InetSocketAddress address,
            ReleaseAble holder,
            SslContext sslContext,
            boolean showRequest,
            boolean showResponse,
            Charset charset) {

        Bootstrap bootstrap = new Bootstrap();
        return bootstrap
                .group(GROUP)
                .channel(NioSocketChannel.class)
                .handler(new HttpPipelineInitializer(
                        holder,
                        sslContext,
                        showRequest,
                        showResponse,
                        charset))
                .connect(address);
    }
}

package httpService.connectors.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import pool.ReleaseAble;

import javax.net.ssl.SSLEngine;
import java.nio.charset.Charset;

public class HttpPipelineInitializer extends ChannelInitializer<Channel> {
    private final boolean sslEnable;
    private final boolean showRequest;
    private final boolean showResponse;
    private final SslContext sslContext;
    private final ReleaseAble holder;

    HttpPipelineInitializer(ReleaseAble holder, SslContext sslContext, boolean showRequest, boolean showResponse) {
        this.sslEnable = sslContext != null;
        this.showRequest = showRequest;
        this.showResponse = showResponse;
        this.holder = holder;
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslEnable && sslContext != null) {
            SSLEngine engine = sslContext.newEngine(ch.alloc());
            pipeline.addFirst(new SslHandler(engine));
        }
        if (showRequest) {
            pipeline.addLast(new WriterHandler());
        }
        if (showResponse) {
            pipeline.addLast(new ReadHandler());
        }
        pipeline.addLast(new HttpClientCodec())
                .addLast(new HttpObjectAggregator(10 * 1024 * 1024))
                .addLast(new HttpResponseHandler(Charset.defaultCharset(), holder));
    }
}

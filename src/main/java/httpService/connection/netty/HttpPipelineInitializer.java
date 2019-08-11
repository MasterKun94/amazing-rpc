package httpService.connection.netty;

import httpService.util.AutoResetChannelPromise;
import httpService.util.ReleaseAble;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import java.nio.charset.Charset;

public class HttpPipelineInitializer extends ChannelInitializer<Channel> {
    private final boolean sslEnable;
    private final boolean showRequest;
    private final boolean showResponse;
    private final SslContext sslContext;
    private final AutoResetChannelPromise promise;
    private final Charset charset;

    HttpPipelineInitializer(
            ReleaseAble holder,
            SslContext sslContext,
            boolean showRequest,
            boolean showResponse,
            Charset charset) {
        this.sslEnable = sslContext != null;
        this.showRequest = showRequest;
        this.showResponse = showResponse;
        this.promise = new AutoResetChannelPromise(holder);
        this.sslContext = sslContext;
        this.charset = charset;
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
        int maxContentLength = 10 * 1024 * 1024;
        pipeline.addLast(new HttpClientCodec())
                .addLast(new HttpObjectAggregator(maxContentLength))
                .addLast(new ExecutionHandler(charset, promise));
    }
}

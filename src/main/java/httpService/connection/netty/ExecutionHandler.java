package httpService.connection.netty;

import httpService.exceptions.CauseType;
import httpService.exceptions.ServerException;
import httpService.exceptions.UnexpectedException;
import httpService.util.AutoResetChannelPromise;
import httpService.util.ResponseFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public class ExecutionHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private final AutoResetChannelPromise promise;
    private final Charset charset;
    private ChannelPipeline pipeline;
    private static final Logger logger = LoggerFactory.getLogger(ExecutionHandler.class);

    public ExecutionHandler(Charset charset, AutoResetChannelPromise promise) {
        this.charset = charset;
        this.promise = promise;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.pipeline = ctx.pipeline();
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        String response = msg.content().toString(charset);
        int status = msg.status().code();
        int status400 = 400;
        if (status >= status400) {
            ServerException exception = ServerException.create(status, response);
            promise.receive(response, exception, exception.getType());
        } else if (!promise.isDone()) {
            promise.receive(msg.content().toString(charset));
        } else {
            ctx.channel().close();
            throw new UnexpectedException(this.toString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!promise.isDone()) {
            promise.receive(cause, CauseType.DEFAULT);
        }
        logger.error(this.toString(), cause);
        ctx.channel().closeFuture();//TODO add listener
    }

    public ResponseFuture<String> executeAsync(FullHttpRequest request) {
        pipeline.writeAndFlush(request);
        return promise;
    }
}

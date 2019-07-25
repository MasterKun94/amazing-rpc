package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import httpService.exceptions.ServerException;
import httpService.exceptions.UnexpectedException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.ChannelHolder;

import java.nio.charset.Charset;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private final AutoResetChannelPromise promise;
    private final Charset charset;

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);

    public HttpResponseHandler(Charset charset, ChannelHolder holder) {
        this.charset = charset;
        this.promise = new AutoResetChannelPromise(holder);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        String response = msg.content().toString(charset);
        int status = msg.status().code();
        if (status >= 400) {
            ServerException exception = ServerException.create(status, response);
            promise.receive(response, exception, exception.getType());
        } else if (!promise.isDone()) {
            ByteBuf byteBuf = msg.content();
            if (byteBuf.writerIndex() == 0) {
                promise.receive("");
            } else {
                promise.receive(byteBuf.toString(charset));
            }
        } else {
            ctx.channel().close();
            throw new UnexpectedException();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!promise.isDone()) {
            logger.error(this.toString(), cause);
//            cause.printStackTrace();//TODO
            promise.receive(cause, CauseType.DEFAULT);
        }
        ctx.channel().closeFuture();//TODO add listener
    }

    public AutoResetChannelPromise getResponseFuture() {
        return this.promise;
    }

}

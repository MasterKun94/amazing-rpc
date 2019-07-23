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
    private final ChannelResponsePromise promise;
    private final Charset charset;

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);

    public HttpResponseHandler(Charset charset, ChannelHolder holder) {
        this.charset = charset;
        this.promise = new ChannelResponsePromise(holder);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        String response = msg.content().toString(charset);
        int status = msg.status().code();
        logger.info("isDone " + promise.isDone());
        if (status >= 400) {
            ServerException exception = ServerException.create(status, response);
            promise.receive(response, exception, exception.getType());
            logger.info("response fail");
        } else if (!promise.isDone()) {
            ByteBuf byteBuf = msg.content();
            if (byteBuf.writerIndex() == 0) {
                promise.receive("");
            } else {
                promise.receive(byteBuf.toString(charset));
            }
            logger.info("response success");

        } else {
            ctx.channel().close();
            logger.info("channel close");

            throw new UnexpectedException();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!promise.isDone()) {
            cause.printStackTrace();//TODO
            promise.receive(cause, CauseType.DEFAULT);
        }
        ctx.channel().closeFuture();//TODO add listener
    }

    public ChannelResponsePromise getResponseFuture() {
        return this.promise;
    }

}

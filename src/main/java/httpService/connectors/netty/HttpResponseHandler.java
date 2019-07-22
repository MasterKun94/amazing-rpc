package httpService.connectors.netty;

import httpService.exceptions.CauseType;
import httpService.exceptions.ServerException;
import httpService.exceptions.UnexpectedException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import pool.ChannelHolder;

import java.nio.charset.Charset;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private final ChannelResponsePromise promise;
    private final Charset charset;

    public HttpResponseHandler(Charset charset, ChannelHolder holder) {
        this.charset = charset;
        this.promise = new ChannelResponsePromise(holder);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        String response = msg.content().toString(charset);
        int status = msg.status().code();
        if (status >= 400) {
            ServerException exception = ServerException.create(status, response);
            promise.receive(response, exception, exception.getType());
        } else if (!promise.isDone()) {
            promise.receive(msg.content().toString(charset));
        } else {
            ctx.channel().close();
            throw new UnexpectedException();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!promise.isDone()) {
            promise.receive(cause, CauseType.DEFAULT);
        }
        ctx.channel().closeFuture();//TODO add listener
    }

    public ChannelResponsePromise getResponseFuture() {
        return this.promise;
    }

}

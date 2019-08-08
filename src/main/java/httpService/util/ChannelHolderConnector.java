package httpService.util;

import httpService.connectors.Connector;
import httpService.connectors.netty.*;
import httpService.exceptions.CauseType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.PoolManager;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class ChannelHolderConnector implements Connector, ReleaseAble {
    private AutoResetChannelPromise future;
    private Channel channel;

    private final int poolIndex;
    private final boolean showRequest;
    private final boolean showResponse;
    private final DefaultArgs defaultArgs;
    private final SslContext sslContext;
    private final Charset charset = CharsetUtil.UTF_8;

    private static final Logger logger = LoggerFactory.getLogger(ChannelHolderConnector.class);

    public ChannelHolderConnector(
            DefaultArgs defaultArgs,
            SslContext sslContext,
            boolean lazy,
            boolean showRequest,
            boolean showResponse,
            int index) {

        this.sslContext = sslContext;
        this.showRequest = showRequest;
        this.showResponse = showResponse;
        this.defaultArgs = defaultArgs;
        this.poolIndex = index;
        if (!lazy) {
            this.channel = Client.start(
                    defaultArgs.getAddress(),
                    null,
                    this,
                    sslContext,
                    showRequest,
                    showResponse);
            this.future = channel.pipeline().get(HttpResponseHandler.class).getFuture();
        }
    }

    @Override
    public <T> ResponseFuture<T> executeAsync(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise) {
        FullHttpRequest request = create(requestArgs, promise);
        Channel channel = getChannel(promise);
        if (promise.isDone()) {
            return null;
        }
        assert channel != null;
        channel.writeAndFlush(request);
        future.addListener(future -> {
            if (future.isDoneAndSuccess()) {
                try {
                    promise.receive(decoder.decode(future.getEntity()));
                } catch (Exception e) {
                    promise.receive(e, CauseType.RESPONSE_DECODE_FAILED);
                }
            } else {
                promise.receive(future.getCause(), future.getCauseType());
            }
        });
        return promise;
    }

    @Override
    public void release() {
        PoolManager.release(defaultArgs.getAddress().toString(), this);
    }

    @Override
    public int getIndex() {
        return poolIndex;
    }

    @Override
    public ResponseFuture<Void> close() {
        ResponsePromise<Void> responsePromise = new ClientResponsePromise<>();
        channel.close().addListener(future -> {
            if (future.isSuccess()) {
                responsePromise.receive(null);
            } else {
                responsePromise.receive(future.cause(), CauseType.DEFAULT);
            }
        });
        return responsePromise;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    private Channel getChannel(ResponsePromise promise) {
        if (this.channel == null || !this.channel.isActive()) {
            this.channel = Client.start(defaultArgs.getAddress(), promise, this, sslContext, showRequest, showResponse);
            if (promise.isDone()) {
                return null;
            }
            this.future = channel.pipeline().get(HttpResponseHandler.class).getFuture();
        }
        return channel;
    }

    private FullHttpRequest create(RequestArgs requestArgs, ResponsePromise promise) {
        if (promise.isDone()) {
            return null;
        }
        Channel channel = getChannel(promise);
        ByteBuf body = getRequestBody(requestArgs.getEntity(), channel);
        String url = getUrl(requestArgs.getPath(), requestArgs.getParam());

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                requestArgs.getMethod().getNettyMethod(),
                url,
                body);

        fullFillHeaders(
                request.headers(),
                requestArgs.getHeaders(),
                defaultArgs.getHeaders(),
                defaultArgs.getAddress(),
                body);

        return request;
    }

    private String getUrl(StringBuilder path, String[][] params) {
        StringBuilder urlBuilder = new StringBuilder(path);
        if (params != null && params.length > 0) {
            boolean isFirst = true;
            for (String[] param : params) {
                urlBuilder.append(isFirst ? '?' : '&');
                isFirst = false;
                urlBuilder.append(param[0]).append('=').append(param[1]);
            }
        }

        return urlBuilder.toString();
    }

    private ByteBuf getRequestBody(String body, Channel channel) {
        ByteBuf byteBuf;
        if (body == null || "".equals(body)) {
            byteBuf = Unpooled.buffer(0);
        } else {
            byte[] bytes = body.getBytes(charset);
            byteBuf = channel.alloc().buffer(bytes.length);
            byteBuf.writeBytes(bytes);

        }
        return byteBuf;
    }

    private void fullFillHeaders(
            HttpHeaders httpHeaders,
            String[][] headers,
            String[][] defaultHeaders,
            InetSocketAddress address,
            ByteBuf byteBuf) {

        if (defaultHeaders != null && defaultHeaders.length > 0) {
            for (String[] defaultHeader : defaultHeaders) {
                httpHeaders.set(defaultHeader[0], defaultHeader[1]);
            }
        }
        if (headers != null && headers.length > 0) {
            for (String[] header : headers) {
                httpHeaders.set(header[0], header[1]);
            }
        }

        httpHeaders.set("Host", address.getHostString() + ":" + address.getPort());
        httpHeaders.set("Content-Length", byteBuf.writerIndex());
        httpHeaders.add("Content-Type", charset);
    }
}

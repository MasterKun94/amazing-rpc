package httpService.util;

import httpService.connection.RpcExecutor;
import httpService.connection.netty.Client;
import httpService.connection.netty.ExecutionHandler;
import httpService.exceptions.CauseType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.PoolManager;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class ChannelHolderExec implements RpcExecutor, ReleaseAble {
    private ExecutionHandler handler;
    private Channel channel;

    private final int poolIndex;
    private final boolean showRequest;
    private final boolean showResponse;
    private final DefaultArgs defaultArgs;
    private final SslContext sslContext;
    private final Charset charset = CharsetUtil.UTF_8;

    private static final Logger logger = LoggerFactory.getLogger(ChannelHolderExec.class);

    public ChannelHolderExec(
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
            InetSocketAddress address = defaultArgs.getAddress();
            ChannelFuture future = Client.start(
                    address,
                    this,
                    sslContext,
                    showRequest,
                    showResponse,
                    charset
            ).syncUninterruptibly();
            if (future.isSuccess()) {
                this.channel = future.channel();
                logger.debug("{}, [{}] connect success", address, channel);
            } else {
                logger.error("{} connect failed", address);
                throw new RuntimeException(future.cause());
            }
            this.handler = channel.pipeline().get(ExecutionHandler.class);
        }
    }

    @Override
    public <T> ResponseFuture<T> executeAsync(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise) {
        FullHttpRequest request = createRequest(requestArgs, promise);
        if (promise.isDone()) {
            return null;
        }
        handler.executeAsync(request)
                .addListener(future -> {
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
        PoolManager.release(defaultArgs.getAddress(), this.poolIndex);
    }

    @Override
    public ResponseFuture<Void> close() {
        ResponsePromise<Void> promise = new ClientResponsePromise<>();
        promise.addListener(future -> {
            if (future.isDoneAndSuccess()) {
                logger.warn("Channel close success, channel holder: [{}]", this);
            } else {
                logger.error("Channel close fail, channel holder: [{}]", this);
                throw new RuntimeException(future.getCause());
            }
        });
        channel.close().addListener(future -> {

            if (future.isSuccess()) {
                promise.receive(null);
            } else {
                promise.receive(future.cause(), CauseType.DEFAULT);
            }
        });

        return promise;
    }

    private boolean channelActive(ResponsePromise promise) {
        if (channel != null && channel.isActive()) {
            return true;
        }

        InetSocketAddress address = defaultArgs.getAddress();
        ChannelFuture future;
        if (this.channel == null) {
            future = Client.start(address,
                    this,
                    sslContext,
                    showRequest,
                    showResponse,
                    charset);
        } else {
            future = channel.connect(address);
        }
        future.addListener(f -> {
            if (future.isSuccess()) {
                logger.debug("{}, [{}] reconnect success", address, future.channel());
            } else {
                promise.receive(future.cause(), CauseType.CONNECTION_CONNECT_FAILED);
                logger.debug("{}, [{}] reconnect failed", address, future.channel());
            }
        }).syncUninterruptibly();

        this.channel = future.channel();
        this.handler = channel.pipeline().get(ExecutionHandler.class);
        return future.isSuccess();
    }

    private FullHttpRequest createRequest(RequestArgs requestArgs, ResponsePromise promise) {
        if (promise.isDone() || !channelActive(promise)) {
            return null;
        }
        ByteBuf body = getRequestBody(requestArgs.getEntity());
        String url = getUrl(requestArgs.getPath(), requestArgs.getParam());

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                requestArgs.getMethod().getNettyMethod(),
                url,
                body);

        fullFillHeaders(request.headers(), requestArgs.getHeaders(), body);

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

    private ByteBuf getRequestBody(String body) {
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
            ByteBuf byteBuf) {
        String[][] defaultHeaders = defaultArgs.getHeaders();
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
        InetSocketAddress address = defaultArgs.getAddress();
        httpHeaders.set("Host", address.getHostString() + ":" + address.getPort());
        httpHeaders.set("Content-Length", byteBuf.writerIndex());
        httpHeaders.add("Content-Type", charset);
    }

    @Override
    public String toString() {
        return "ChannelHolderConnector(channel=" + this.channel + ", poolIndex=" + this.poolIndex + ", showRequest=" + this.showRequest + ", showResponse=" + this.showResponse + ", defaultArgs=" + this.defaultArgs + ", sslContext=" + this.sslContext + ", charset=" + this.charset + ")";
    }
}

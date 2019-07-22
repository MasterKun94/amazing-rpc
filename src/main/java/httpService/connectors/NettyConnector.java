package httpService.connectors;

import httpService.DefaultArgs;
import httpService.RequestArgs;
import httpService.connectors.netty.ResponsePromise;
import httpService.proxy.ResponseDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import pool.ChannelHolder;
import pool.ChannelManager;

import java.nio.charset.Charset;
import java.util.Map;

public class NettyConnector implements Connector {
    private Charset charset = CharsetUtil.UTF_8;

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    @Override
    public <T> T execute(RequestArgs requestArgs, ResponsePromise<T> promise, ResponseDecoder<T> decoder) throws Throwable {
        ResponsePromise<T> future = executeAsync(requestArgs, promise, decoder);
        if (future.whenSuccess(requestArgs.getTimeout())) {
            return future.getEntityAndReset();
        } else {
            throw promise.getCause();
        }
    }

    @Override
    public <T> ResponsePromise<T> executeAsync(RequestArgs requestArgs, ResponsePromise<T> promise, ResponseDecoder<T> decoder) {
        if (promise.isDoneAndFailed()) {
            return null;
        }

        ChannelHolder holder = ChannelManager.alloc(getAddress(requestArgs), promise);
        if (promise.isDoneAndFailed())
            return null;//TODO

        assert holder != null;
        FullHttpRequest request = create(holder, requestArgs, promise);
        if (promise.isDoneAndFailed())
            return null;

        return holder.executeAsync(request, promise, decoder);

    }

    private FullHttpRequest create(ChannelHolder holder, RequestArgs requestArgs, ResponsePromise promise) {
        DefaultArgs defaultArgs = holder.getDefaultArgs();
        ByteBuf body = getRequestBody(requestArgs.getEntity(), holder.getChannel(promise));
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

    private String getAddress(RequestArgs requestArgs) {
        return requestArgs.getHost() + ":" + requestArgs.getPort();
    }

    private String getUrl(String[] path, Map<String, String> param) {
        StringBuilder urlBuilder = new StringBuilder();
        for (String s : path) {
            if (!StringUtil.isNullOrEmpty(s))
                urlBuilder.append('/').append(s);
        }
        if (param != null && !param.isEmpty()) {
            boolean isFirst = true;
            for (Map.Entry<String, String> entry : param.entrySet()) {
                urlBuilder.append(isFirst ? '?' : '&');
                isFirst = false;
                urlBuilder.append(entry.getKey()).append('=').append(entry.getValue());
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
            Map<String, String> headers,
            Map<String, String> defaultHeaders,
            String address,
            ByteBuf byteBuf) {

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpHeaders::set);
        }
        if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
            defaultHeaders.forEach(httpHeaders::set);
        }
        httpHeaders.set("Host", address);
        httpHeaders.set("Content-Length", byteBuf.writerIndex());
        httpHeaders.add("Content-Type", charset);
    }
}

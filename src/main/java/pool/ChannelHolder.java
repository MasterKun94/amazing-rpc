package pool;

import httpService.DefaultArgs;
import httpService.connectors.netty.*;
import httpService.exceptions.CauseType;
import httpService.proxy.ResponseDecoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;

public class ChannelHolder {
    private Channel channel;
    private final DefaultArgs defaultArgs;
    private int poolIndex = -1;
    private HttpResponseHandler responseHandler;
    private boolean showRequest;
    private boolean showResponse;

    private static final Logger logger = LoggerFactory.getLogger(ChannelHolder.class);

    ChannelHolder(DefaultArgs defaultArgs, boolean lazy, boolean showRequest, boolean showResponse) {
        this.showRequest = showRequest;
        this.showResponse = showResponse;
        if (!lazy) {
            this.channel = Client.start(defaultArgs.getIp(), defaultArgs.getPort(), null, this, showRequest, showResponse);
            this.responseHandler = channel.pipeline().get(HttpResponseHandler.class);
        }
        this.defaultArgs = defaultArgs;
    }

    public Channel getChannel(ResponsePromise promise) {
        String ip = defaultArgs.getIp();
        int port = defaultArgs.getPort();
        if (channel == null) {
            this.channel = Client.start(ip, port, promise, this, showRequest, showResponse);
            this.responseHandler = channel.pipeline().get(HttpResponseHandler.class);
        } else if (!channel.isActive()) {
            this.channel.connect(new InetSocketAddress(ip, port))
                    .syncUninterruptibly();//TODO addListener
            this.responseHandler = channel.pipeline().get(HttpResponseHandler.class);
        }
        return channel;
    }

    public <T> ResponsePromise<T> executeAsync(FullHttpRequest request, ResponsePromise<T> promise, ResponseDecoder<T> decoder) {
        Channel channel = getChannel(promise);
        if (promise.isDone())
            return null;

        channel.writeAndFlush(request);
        AutoResetChannelPromise channelPromise = responseHandler.getResponseFuture();
        channelPromise.addListener(future -> {
            if (future.isDoneAndSuccess()) {
                try {
                    T t = decoder.decode(future.getEntity());
                    promise.receive(t);
                } catch (Exception e) {
                    promise.receive(e, CauseType.RESPONSE_DECODE_FAILED);
                }
            } else {
                promise.receive(future.getCause(), future.getCauseType());
            }
        });
        return promise;
    }

    public DefaultArgs getDefaultArgs() {
        return defaultArgs;
    }

    public int releaseFrom(Map<String, ChannelPool> poolParty) {
        return poolParty.get(defaultArgs.getAddress()).release(poolIndex);
    }

    public void setPoolIndex(int index) {
        this.poolIndex = index;
    }

    public ChannelFuture close() {
        return channel.close();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

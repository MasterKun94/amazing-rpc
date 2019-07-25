package pool;

import httpService.connectors.netty.ResponsePromise;
import httpService.exceptions.CauseType;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);

    private static final Map<String, ChannelPool> POOL_PARTY = new HashMap<>();

    public static ChannelHolder alloc(String host, ResponsePromise promise) {
        if (promise.isDone()) {
            return null;
        }
        ChannelPool pool = POOL_PARTY.get(host);
        try {
            int index = pool.getIndex();
            ChannelHolder holder = pool.getElement(index);
            logger.debug("Channel holder pull success, target pool: [{}], " +
                    "channel holder: [{}]", pool, holder);
            return holder;
        } catch (Exception e) {
            logger.warn("Channel holder pull failed, exception is: {}, message: {}, " +
                    "target pool: [{}]", e.getClass(), e.getMessage(), pool);
            promise.receive(e, CauseType.CHANNELPOOL_PULL_FAILED);
            return null;
        }
    }

    public static void release(ChannelHolder holder) {
        logger.debug("Channel holder releasing: [{}]", holder);
        int i;
        do {
            i = holder == null ? -1 : holder.releaseFrom(POOL_PARTY);
        } while (i > 0);
        logger.debug("Channel holder release success");
    }

    public static void close(ChannelHolder holder) {
        ChannelFuture channelFuture = holder.close();
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                logger.warn("Channel close success: [{}], channel holder: [{}]",
                        channelFuture.channel(), holder);
            } else {
                logger.error("Channel close fail: [{}], channel holder: [{}]",
                        channelFuture.channel(), holder);
                throw new RuntimeException(future.cause());
            }
        });
    }

    public static boolean exist(String host) {
        return POOL_PARTY.containsKey(host);
    }

    public static synchronized void register(String host, Supplier<ChannelPool> pool) {
        if (exist(host)) {
            return;
        }
        POOL_PARTY.put(host, pool.get());
    }
}

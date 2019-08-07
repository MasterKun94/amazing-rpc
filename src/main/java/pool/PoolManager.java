package pool;

import httpService.connectors.Connector;
import httpService.exceptions.ChannelPoolException;
import httpService.proxy.ResponsePromise;
import httpService.exceptions.CauseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PoolManager {

    private static final Logger logger = LoggerFactory.getLogger(PoolManager.class);

    private static final Map<InetSocketAddress, ChannelPool> POOL_PARTY = new ConcurrentHashMap<>();

    public static Connector alloc(InetSocketAddress address, ResponsePromise promise) {
        if (promise.isDone()) {
            return null;
        }
        ChannelPool pool = POOL_PARTY.get(address);
        try {
            Connector holder = pool.get();
            logger.debug("Channel holder pull success, target pool: [{}], " +
                    "channel holder: [{}]", pool, holder);
            return holder;
        } catch (Exception e) {
            logger.warn("Channel holder pull failed, exception is: {}, message: {}, " +
                    "target pool: [{}]", e.getClass(), e.getMessage(), pool);
            promise.receive(new ChannelPoolException.PullTimeout(e), CauseType.CHANNELPOOL_PULL_FAILED);
            return null;
        }
    }

    public static void release(InetSocketAddress address, int index) {
        logger.debug("Channel holder releasing: [{}, {}]", address, index);
        int i;
        do {
            i = index == -1 ? -1 : POOL_PARTY.get(address).release(index);
        } while (i > 0);
        logger.debug("Channel holder release success");
    }

    public static boolean exist(InetSocketAddress address) {
        return POOL_PARTY.containsKey(address);
    }

    public static synchronized void subscribe(InetSocketAddress address, Supplier<ChannelPool> pool) {
        POOL_PARTY.computeIfAbsent(address, k -> POOL_PARTY.put(k, pool.get()));
    }
}

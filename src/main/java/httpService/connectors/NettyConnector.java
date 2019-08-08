package httpService.connectors;

import httpService.util.RequestArgs;
import httpService.exceptions.UnexpectedException;
import httpService.util.Decoder;
import httpService.util.ResponseFuture;
import httpService.util.ResponsePromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.PoolManager;

public class NettyConnector implements Connector {
    private static final Logger logger = LoggerFactory.getLogger(NettyConnector.class);

    @Override
    public <T> ResponseFuture<T> executeAsync(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise) {
        String address = requestArgs.getAddress().toString();
        Connector holder = PoolManager.alloc(address, promise);
        if (promise.isDone()){
            return promise;
        } else if (holder != null) {
            return holder.executeAsync(requestArgs, decoder, promise);
        } else {
            throw new UnexpectedException(this.toString());
        }
    }
}

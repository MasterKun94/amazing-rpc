package httpService.connection;

import httpService.util.RequestArgs;
import httpService.exceptions.UnexpectedException;
import httpService.util.Decoder;
import httpService.util.ResponseFuture;
import httpService.util.ResponsePromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.PoolManager;

public class NettyExec implements RpcExecutor {
    private static final Logger logger = LoggerFactory.getLogger(NettyExec.class);

    @Override
    public <T> ResponseFuture<T> executeAsync(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise) {
        RpcExecutor rpcExecutor = PoolManager.alloc(requestArgs.getAddress(), promise);
        if (promise.isDone()){
            return promise;
        } else if (rpcExecutor != null) {
            return rpcExecutor.executeAsync(requestArgs, decoder, promise);
        } else {
            throw new UnexpectedException(this.toString());
        }
    }
}

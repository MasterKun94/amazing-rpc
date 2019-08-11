package httpService.connection;

import httpService.util.RequestArgs;
import httpService.util.*;
import io.netty.handler.ssl.SslContext;
import pool.PoolManager;
import pool.ChannelPool;
import pool.poolUtil.Box;

import java.net.InetSocketAddress;
//import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

public class ConnectorBuilder {
    private Supplier<RpcExecutor> supplier;

    public static NettyConnectorBuilder createNetty(List<InetSocketAddress> address) {
        return new NettyConnectorBuilder(address);
    }

    public static ConnectorBuilder createHttpClient() {
        ConnectorBuilder builder = new ConnectorBuilder();
        builder.supplier = HttpClientExec::new;
        return builder;
    }

    public RpcExecutor build() {
        return supplier.get();
    }

    public static class NettyConnectorBuilder extends ConnectorBuilder {
        private List<InetSocketAddress> socketAddresses;
        private int capacity = 32;
        private boolean lazy = false;
        private boolean showRequest = false;
        private boolean showResponse = false;
        private Map<String, String> headers = new HashMap<>();
        private BlockingQueue<Box> queue;
        private SslContext sslContext;
        private List<DecoratorInitializer> initializers;

        private NettyConnectorBuilder(List<InetSocketAddress> socketAddresses) {
            this.socketAddresses = socketAddresses;
        }

        public NettyConnectorBuilder setPoolCapacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public NettyConnectorBuilder setLazyInit(boolean lazy) {
            this.lazy = lazy;
            return this;
        }

        public NettyConnectorBuilder setShowRequest(boolean showRequest) {
            this.showRequest = showRequest;
            return this;
        }

        public NettyConnectorBuilder setShowResponse(boolean showResponse) {
            this.showResponse = showResponse;
            return this;
        }

        public NettyConnectorBuilder setHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public NettyConnectorBuilder setQueue(BlockingQueue<Box> blockingQueue) {
            this.queue = blockingQueue;
            return this;
        }

        public NettyConnectorBuilder setSslContext(SslContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public NettyConnectorBuilder setMonitors(List<DecoratorInitializer> inits) {
            this.initializers = inits;
            return this;
        }

        @Override
        public RpcExecutor build() {
            for (InetSocketAddress socketAddress : this.socketAddresses) {
                String[][] defaultHeaders = new String[headers.size()][2];
                int i = 0;
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    defaultHeaders[i][0] = entry.getKey();
                    defaultHeaders[i][1] = entry.getValue();
                    i++;
                }

                if (!PoolManager.exist(socketAddress)) {
                    Supplier<ChannelPool> pool = () -> new ChannelPool(
                            socketAddress,
                            capacity,
                            lazy,
                            sslContext,
                            defaultHeaders,
                            queue,
                            showRequest,
                            showResponse
                    );
                    PoolManager.subscribe(socketAddress, pool);
                }
            }
            RpcExecutor rpcExecutor = new NettyExec();
            if (initializers != null && !initializers.isEmpty()) {
                for (DecoratorInitializer initializer : initializers) {
                    rpcExecutor = decorateConnector(rpcExecutor, initializer.init());
                }
            }

            return rpcExecutor;
        }

        private static RpcExecutor decorateConnector(RpcExecutor rpcExecutor, Decorator decorator) {
            return new RpcExecutor() {
                @Override
                public <T> ResponseFuture<T> executeAsync(RequestArgs args, Decoder<T> decoder, ResponsePromise<T> promise) {
                    decorator.beforeSendRequest();
                    return rpcExecutor.executeAsync(args, decoder, promise)
                            .addListener(decorator::afterReceiveResponse);
                }
            };
        }
    }


}

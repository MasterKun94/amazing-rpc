package httpService.connectors;

import httpService.proxy.RequestArgs;
import httpService.proxy.*;
import io.netty.handler.ssl.SslContext;
import pool.PoolManager;
import pool.ChannelPool;
import pool.poolUtil.Box;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

public class ConnectorBuilder {
    private Supplier<Connector> supplier;

    public static NettyConnectorBuilder createNetty(List<InetSocketAddress> address) {
        return new NettyConnectorBuilder(address);
    }

    public static ConnectorBuilder createHttpClient() {
        ConnectorBuilder builder = new ConnectorBuilder();
        builder.supplier = HttpClientConnector::new;
        return builder;
    }

    public Connector build() {
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
        private List<MonitorInitializer> initializers;

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

        public NettyConnectorBuilder setMonitors(List<MonitorInitializer> inits) {
            this.initializers = inits;
            return this;
        }

        @Override
        public Connector build() {
            for (InetSocketAddress socketAddress : this.socketAddresses) {
                String[][] defaultHeaders = new String[headers.size()][2];
                int i = 0;
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    defaultHeaders[i][0] = entry.getKey();
                    defaultHeaders[i][1] = entry.getValue();
                    i++;
                }

                if (!PoolManager.exist(socketAddress.toString())) {
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
                    PoolManager.register(socketAddress.toString(), pool);
                }
            }
            Connector connector = new NettyConnector();
            if (initializers != null && !initializers.isEmpty()) {
                for (MonitorInitializer initializer : initializers) {
                    connector = decorateConnector(connector, initializer.init());
                }
            }
            return connector;
        }

        private static Connector decorateConnector(Connector connector, Monitor monitor) {
            return new Connector() {
                @Override
                public <T> ResponseFuture<T> executeAsync(RequestArgs args, Decoder<T> decoder, ResponsePromise<T> promise) {
                    monitor.beforeSendRequest();
                    return connector.executeAsync(args, decoder, promise)
                            .addListener(monitor::afterReceiveResponse);
                }
            };
        }
    }


}

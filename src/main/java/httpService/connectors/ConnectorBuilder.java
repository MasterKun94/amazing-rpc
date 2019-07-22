package httpService.connectors;

import httpService.proxy.Host;
import pool.ChannelManager;
import pool.ChannelPool;
import pool.util.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

public class ConnectorBuilder {
    private Supplier<Connector> supplier;

    public static NettyConnectorBuilder createNetty(List<Host> hosts) {
        return new NettyConnectorBuilder(hosts);
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
        private List<Host> hosts;
        private int capacity = 32;
        private boolean lazy = false;
        private boolean showReqeust = false;
        private boolean showResponse = false;
        private Map<String, String> headers = new HashMap<>();
        private BlockingQueue<Box> queue;

        private NettyConnectorBuilder(List<Host> hosts) {
            this.hosts = hosts;
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
            this.showReqeust = showRequest;
            return this;
        }

        public NettyConnectorBuilder setShowResponse(boolean showResponse) {
            this.showResponse = showResponse;
            return this;
        }

        public NettyConnectorBuilder setHeader(String name, String value) {
            this.headers.put(name, value);
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

        @Override
        public Connector build() {
            for (Host host : hosts) {
                if (!ChannelManager.exist(host.getHost())) {
                    Supplier<ChannelPool> pool = () -> new ChannelPool(
                            host.getIp(),
                            host.getPort(),
                            capacity,
                            lazy,
                            headers,
                            queue,
                            showReqeust,
                            showResponse
                    );
                    ChannelManager.register(host.getHost(), pool);
                }
            }
            return new NettyConnector();
        }
    }
}

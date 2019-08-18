package httpService.connection;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import httpService.exceptions.CauseType;
import httpService.util.ClientResponsePromise;
import httpService.util.Decoder;
import httpService.util.HttpMethod;
import httpService.util.LoadBalancer;
import httpService.util.Methods;
import httpService.util.RequestArgs;
import httpService.util.ResponseFuture;
import httpService.util.ResponsePromise;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RpcClient {
    private RpcExecutor executor;
    private LoadBalancer loadBalancer;

    public RpcClient(RpcExecutor executor, LoadBalancer loadBalancer) {
        this.executor = executor;
        this.loadBalancer = loadBalancer;
    }

    public <T> RequestExecutor<T> createExecutor(HttpMethod method, String path, TypeReference<T> returnType) {
        return new RequestExecutor<>(method, path, returnType.getType());

    }

    public <T> RequestExecutor<T> get(String path, TypeReference<T> returnType) {
        return new RequestExecutor<>(HttpMethod.GET, path, returnType.getType());

    }

    public <T> RequestExecutor<T> post(String path, TypeReference<T> returnType) {
        return new RequestExecutor<>(HttpMethod.POST, path, returnType.getType());

    }

    public <T> RequestExecutor<T> put(String path, TypeReference<T> returnType) {
        return new RequestExecutor<>(HttpMethod.PUT, path, returnType.getType());

    }

    public <T> RequestExecutor<T> delete(String path, TypeReference<T> returnType) {
        return new RequestExecutor<>(HttpMethod.DELETE, path, returnType.getType());
    }

    public class RequestExecutor<T> {
        private volatile Decoder<T> decoder;
        private HttpMethod method;
        private String path;
        private Type type;

        public RequestExecutor(HttpMethod method, String path, Type type) {
            this.method = method;
            this.path = path;
            this.type = type;
            try {
                this.decoder = Methods.decode(type);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        public ResponseFuture<T> execute(String[][] params, String[][] headers, Object body) {//TODO
            RequestArgs requestArgs = new RequestArgs();
            requestArgs.setAddress(loadBalancer);
            requestArgs.setMethod(method);
            requestArgs.setEntity(JSON.toJSONString(body));
            requestArgs.setPath(new StringBuilder(path));
            requestArgs.setHeaders(headers);
            requestArgs.setParams(params);

            return execute(requestArgs);
        }

        public ResponseFuture<T> execute(RequestArgs requestArgs) {
            ResponsePromise<T> promise = new ClientResponsePromise<>();
            return executor.executeAsync(requestArgs, decoder, promise);
        }

        public HttpMethod getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public Type getReturnType() {
            return type;
        }

        public LoadBalancer getLoadBalancer() {
            return loadBalancer;
        }
    }

    public static void main(String[] args) throws Exception {

    }
}

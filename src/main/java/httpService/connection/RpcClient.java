package httpService.connection;

import com.alibaba.fastjson.JSON;
import httpService.exceptions.CauseType;
import httpService.util.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RpcClient {
    private RpcExecutor executor;
    private LoadBalancer loadBalancer;

    public RpcClient(RpcExecutor executor, LoadBalancer loadBalancer) {
        this.executor = executor;
        this.loadBalancer = loadBalancer;
    }

    public <T> RequestExecutor<T> get(String path) {
        RequestExecutor executor = new RequestExecutor();
        executor.method = HttpMethod.GET;
        executor.path = path;
        return executor;
    }

    public <T> RequestExecutor<T> post(String path) {
        RequestExecutor executor = new RequestExecutor();
        executor.method = HttpMethod.POST;
        executor.path = path;
        return executor;
    }

    public <T> RequestExecutor<T> put(String path) {
        RequestExecutor executor = new RequestExecutor();
        executor.method = HttpMethod.PUT;
        executor.path = path;
        return executor;
    }

    public <T> RequestExecutor<T> delete(String path) {
        RequestExecutor executor = new RequestExecutor();
        executor.method = HttpMethod.DELETE;
        executor.path = path;
        return executor;
    }

    public class RequestExecutor<T> {//TODO
        private volatile Decoder<T> decoder;
        private String path;
        private HttpMethod method;
        private List<String[]> headers = new ArrayList<>(5);
        private List<String[]> params = new ArrayList<>(5);
        private Object body;

        public RequestExecutor setBody(Object body) {
            this.body = body;
            return this;
        }

        public RequestExecutor setParam(String name, String value) {
            params.add(new String[]{name, value});
            return this;
        }

        public RequestExecutor setHeaders(String name, String value) {
            headers.add(new String[]{name, value});
            return this;
        }

        public ResponseFuture<T> execute() {
            if (decoder == null) {
                try {
                    synchronized (this) {
                        if (decoder == null) {
                            Method method = this.getClass().getMethod("execute");
                            decoder = Methods.decode(method);
                        }
                    }
                } catch (Exception e) {
                    ResponsePromise<T> promise = new ClientResponsePromise<>();
                    promise.receive(e, CauseType.DEFAULT);
                    return promise;
                }
            }
            return execute(decoder);
        }

        public ResponseFuture<T> execute(Decoder<T> decoder) {
            RequestArgs requestArgs = new RequestArgs();
            requestArgs.setAddress(loadBalancer);
            requestArgs.setMethod(method);
            requestArgs.setEntity(JSON.toJSONString(body));
            requestArgs.setPath(new StringBuilder(path));

            if (this.headers != null) {
                String[][] headers = new String[this.headers.size()][2];
                this.headers.toArray(headers);
                requestArgs.setHeaders(headers);
            }
            if (this.params != null) {
                String[][] params = new String[this.params.size()][2];
                this.params.toArray(params);
                requestArgs.setParams(params);
            }
            ResponsePromise<T> promise = new ClientResponsePromise<>();

            return executor.executeAsync(requestArgs, decoder, promise);//TODO
        }
    }
}

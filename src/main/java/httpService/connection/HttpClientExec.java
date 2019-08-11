package httpService.connection;

import httpService.connection.httpClient.HttpBuilder;
import httpService.connection.httpClient.HttpConnector;
import httpService.exceptions.CauseType;
import httpService.util.*;
import org.apache.http.util.EntityUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HttpClientExec implements RpcExecutor {
    private Executor executor = Executors.newFixedThreadPool(20);

    public <T> void doExecute(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise) {
        InetSocketAddress socketAddress = requestArgs.getAddress();

        String url = "http://" +
                socketAddress.toString() +
                ":" +
                socketAddress.getPort() +
                requestArgs.getPath();
//        System.out.println(url);
        HttpBuilder builder;
        switch (requestArgs.getMethod()) {
            case GET:
                builder = HttpBuilder.get(url);
                break;
            case POST:
                builder = HttpBuilder.post(url);
                break;
            case PUT:
                builder = HttpBuilder.put(url);
                break;
            case DELETE:
                builder = HttpBuilder.delete(url);
                break;
            default:
                throw new IllegalArgumentException();
        }
        builder.rest();
        if (requestArgs.getParam() != null) {
            builder.params(requestArgs.getParam());
        }
        if (requestArgs.getHeaders() != null) {
            builder.headers(requestArgs.getHeaders());
        }
        if (requestArgs.getEntity() != null && !"".equals(requestArgs.getEntity())) {
            builder.entity(requestArgs.getEntity());
        }

        String response = builder
                .execute(HttpConnector
                        .of((req, res) -> EntityUtils.toString(res.getEntity())))
                .sync();
        try {
            promise.receive(decoder.decode(response));
        } catch (Exception e) {
            promise.receive(e, CauseType.DEFAULT);
        }
    }

    @Override
    public <T> ResponseFuture<T> executeAsync(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise) {
        executor.execute(() -> doExecute(requestArgs, decoder, promise));
        return promise;
    }
}

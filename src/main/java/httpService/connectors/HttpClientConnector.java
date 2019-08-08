package httpService.connectors;

import httpService.connectors.httpClient.HttpBuilder;
import httpService.connectors.httpClient.HttpConnector;
import httpService.util.RequestArgs;
import httpService.util.Decoder;
import httpService.util.ResponseFuture;
import httpService.util.ResponsePromise;
import org.apache.http.util.EntityUtils;

import java.net.InetSocketAddress;

public class HttpClientConnector implements Connector {

    public <T> T execute(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise) throws Exception {
        InetSocketAddress socketAddress = requestArgs.getAddress();
        StringBuilder stringBuilder = new StringBuilder()
                .append("http://")
                .append(socketAddress.toString())
                .append(":")
                .append(socketAddress.getPort())
                .append(requestArgs.getPath());

        String url = stringBuilder.toString();
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
        return decoder.decode(response);
    }

    @Override
    public <T> ResponseFuture<T> executeAsync(RequestArgs requestArgs, Decoder<T> decoder, ResponsePromise<T> promise) {
        return null;//TODO
    }
}

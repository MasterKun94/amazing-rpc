package httpService.connectors;

import httpService.connectors.httpClient.HttpBuilder;
import httpService.connectors.httpClient.HttpConnector;
import httpService.RequestArgs;
import httpService.connectors.netty.ResponsePromise;
import httpService.proxy.ResponseDecoder;
import org.apache.http.util.EntityUtils;

public class HttpClientConnector implements Connector {

    @Override
    public <T> T execute(RequestArgs requestArgs, ResponseDecoder<T> decoder, ResponsePromise<T> promise) throws Exception {

        StringBuilder stringBuilder = new StringBuilder()
                .append("http://")
                .append(requestArgs.getHost())
                .append(":")
                .append(requestArgs.getPort());
        for (String s : requestArgs.getPath()) {
            if (s != null && !s.equals(""))
                stringBuilder.append("/").append(s);
        }
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
    public <T> ResponsePromise<T> executeAsync(RequestArgs requestArgs, ResponseDecoder<T> decoder, ResponsePromise<T> promise) {
        return null;//TODO
    }
}

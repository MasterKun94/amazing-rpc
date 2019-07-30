package httpService.connectors.httpClient;

import org.apache.commons.codec.Charsets;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 一个http请求建造器
 */
public class HttpBuilder {

    private HttpRequestBase request;
    private StringBuilder urlBuilder;
    private boolean haveParam;

    private static HttpBuilder start(HttpRequestBase request, String url) {

        HttpBuilder builder = new HttpBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        builder.urlBuilder = url.startsWith("http://") ?
                stringBuilder.append(url) :
                stringBuilder.append("http://").append(url);
        builder.request = request;
        return builder;
    }

    public static HttpBuilder post(String url) {
        return start(new HttpPost(), url);
    }

    public static HttpBuilder get(String url) {
        return start(new HttpGet(), url);
    }

    public static HttpBuilder put(String url) {
        return start(new HttpPut(), url);
    }

    public static HttpBuilder delete(String url) {
        return start(new HttpDelete(), url);
    }

    public HttpBuilder params(String[][] params) {
        if (params != null && params.length > 0) {
            for (String[] param : params) {
                param(param[0], param[1]);
            }
        }
        return this;
    }

    public HttpBuilder param(String key, String value) {
        if (value != null && !"".equals(value)) {
            urlBuilder.append(haveParam ? "&" : "?")
                    .append(key).append("=").append(value);
            haveParam = true;
        }
        return this;
    }

    public HttpBuilder headers(String[][] headers) {
        if (headers != null && headers.length > 0) {
            for (String[] header : headers) {
                request.setHeader(header[0], header[1]);
            }
        }
        return this;
    }

    public HttpBuilder header(String head, String value) {
        if (value != null && !"".equals(value)) {
            request.setHeader(head, value);
        }
        return this;
    }

    public HttpBuilder rest() {
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        return this;
    }

    public HttpBuilder entity(String entity) {
        if (entity != null) {
            if (request instanceof HttpPost || request instanceof HttpPut) {
                ((HttpEntityEnclosingRequestBase) request)
                        .setEntity(new StringEntity(entity, Charsets.UTF_8));
            } else {
                throw new UnsupportedOperationException(
                        request.getMethod() + " method not supported");
            }
        }
        return this;
    }

    public HttpBuilder charset(Charset charset) {
        request.setHeader("Charset", charset.toString());
        return this;
    }

    public <T> HttpResponseBuilder<T> execute(HttpConnector<T> connector) {
        request.setURI(URI.create(urlBuilder.toString()));
        return new HttpResponseBuilder<>(() -> connector.execute(request));
    }

    public <T> HttpResponseBuilder<T> execute(Function<String, T> handler) {
        request.setURI(URI.create(urlBuilder.toString()));
        HttpConnector<T> connector = HttpConnector.of((req, res) ->
                handler.apply(EntityUtils.toString(res.getEntity())));
        return new HttpResponseBuilder<>(() -> connector.execute(request));
    }

    public class HttpResponseBuilder<T> {
        private Supplier<T> supplier;

        HttpResponseBuilder(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public CompletableFuture<T> async(Executor executor) {
            return CompletableFuture.supplyAsync(supplier, executor);
        }

        public T sync() {
            return supplier.get();
        }
    }
}

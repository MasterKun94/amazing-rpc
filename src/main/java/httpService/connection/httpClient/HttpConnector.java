package httpService.connection.httpClient;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Objects;

public class HttpConnector<T> {
    private CloseableHttpClient httpClient;
    private HttpContext httpContext;
    private HttpHost httpHost;
    private ResponseHandler<T> responseHandler;


    public T execute(HttpUriRequest request) {
        if (httpClient == null) httpClient = InstanceHttpClient.getDefault();
        if (httpHost == null) httpHost = URIUtils.extractHost(request.getURI());
        org.apache.http.client.ResponseHandler<T> responseHandler = response -> {
            try {
                return this.responseHandler.handleResponse(request, response);
            } catch (IOException e) {
                return this.responseHandler.handleException(request, e);
            }
        };
        try {
            return httpClient.execute(
                    httpHost,
                    request,
                    responseHandler,
                    httpContext);
        } catch (IOException e) {
            return this.responseHandler.handleException(request, e);
        }
    }

    private HttpConnector() {}

    public static <T> HttpConnector<T> of(ResponseHandler<T> responseHandler) {
        Objects.requireNonNull(responseHandler);
        HttpConnector<T> connector = new HttpConnector<>();
        connector.responseHandler = responseHandler;
        return connector;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    public void setHttpHost(HttpHost httpHost) {
        this.httpHost = httpHost;
    }
}

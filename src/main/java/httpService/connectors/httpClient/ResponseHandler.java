package httpService.connectors.httpClient;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.IOException;

public interface ResponseHandler<T> {

    T handleResponse(HttpRequest request, HttpResponse response) throws IOException;

    default T handleException(HttpRequest request, Exception e) {
        throw new RuntimeException(e);
    }
}

package httpService.util;

public interface HttpRequest {

    String getHost();

    String getPath();

    HttpMethod getMethod();

    String getHeader(String name);

    int getBodySize();
}

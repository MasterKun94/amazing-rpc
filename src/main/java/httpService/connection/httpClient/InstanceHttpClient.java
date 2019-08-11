package httpService.connection.httpClient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class InstanceHttpClient {
    private static volatile CloseableHttpClient httpClient;

    public static CloseableHttpClient getDefault() {
        if (httpClient == null) {
            synchronized (InstanceHttpClient.class) {
                if (httpClient == null) {
                    httpClient = HttpClients.createDefault();
                }
            }
        }
        return httpClient;
    }
}
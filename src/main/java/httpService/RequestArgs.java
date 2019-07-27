package httpService;

import httpService.connectors.LoadBalancer;

import java.util.Map;

public class RequestArgs {
    private LoadBalancer loadBalancer;
    private String[] path;
    private HttpMethod method;
    private Map<String, String> param;
    private Map<String, String> headers;
    private String entity;
    private long timeout;

    public RequestArgs() {}

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public String[] getPath() {
        return this.path;
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public Map<String, String> getParam() {
        return this.param;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public String getEntity() {
        return this.entity;
    }

    public long getTimeout() {
        return this.timeout;
    }

    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public void setPath(String[] path) {
        this.path = path;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public void setParam(Map<String, String> param) {
        this.param = param;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}

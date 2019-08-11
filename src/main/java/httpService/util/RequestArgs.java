package httpService.util;

import java.net.InetSocketAddress;

public class RequestArgs {
    private InetSocketAddress address;
    private StringBuilder path;
    private HttpMethod method;
    private String[][] param;
    private String[][] headers;
    private String entity;

    public RequestArgs() {}

    public InetSocketAddress getAddress() {
        return address;
    }

    public StringBuilder getPath() {
        return this.path;
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public String[][] getParam() {
        return this.param;
    }

    public String[][] getHeaders() {
        return this.headers;
    }

    public String getEntity() {
        return this.entity;
    }

    public void setAddress(LoadBalancer balancer) {
        this.address = balancer.select();
    }

//    public void setAddress(InetSocketAddress address) {
//        this.address = address;
//    }

    public void setPath(StringBuilder path) {
        this.path = path;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public void setParams(String[][] param) {
        this.param = param;
    }

    public void setHeaders(String[][] headers) {
        this.headers = headers;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

}

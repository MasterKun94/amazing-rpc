package httpService.proxy;

public enum HttpMethod {
    GET(io.netty.handler.codec.http.HttpMethod.GET),
    POST(io.netty.handler.codec.http.HttpMethod.POST),
    PUT(io.netty.handler.codec.http.HttpMethod.PUT),
    DELETE(io.netty.handler.codec.http.HttpMethod.DELETE);

    private io.netty.handler.codec.http.HttpMethod nettyMethod;

    HttpMethod(io.netty.handler.codec.http.HttpMethod nettyMethod) {
        this.nettyMethod = nettyMethod;
    }

    public io.netty.handler.codec.http.HttpMethod getNettyMethod() {
        return nettyMethod;
    }
}

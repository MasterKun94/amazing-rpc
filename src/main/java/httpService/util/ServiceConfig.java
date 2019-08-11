package httpService.util;

import httpService.annotation.RequestHeaders;
import httpService.annotation.ServiceContext;
import httpService.connection.ConnectorType;

import java.util.HashMap;
import java.util.Map;

import static httpService.util.AliasUtil.parse;

public class ServiceConfig {
    private String host;
    private String contextPath;
    private long timeout = 15000;
    private int poolCapacity = 32;
    private boolean lazyInit = false;
    private boolean showRequest = false;
    private boolean showResponse = false;
    private ConnectorType connector = ConnectorType.NETTY;
    private Map<String, String> defaultHeaders;
    private SslConfig sslConfig;

    public static ServiceConfig parseContext(ServiceContext context) {
        ServiceConfig config = new ServiceConfig();
        config.setHost(parse(context, "host"));
        config.setContextPath(parse(context, "contextPath"));
        config.setTimeout(parse(context, "timeout"));
        config.setPoolCapacity(parse(context, "poolCapacity"));
        config.setLazyInit(parse(context, "lazyInit"));
        config.setShowRequest(parse(context, "showRequest"));
        config.setShowResponse(parse(context, "showResponse"));
        config.setConnector(parse(context, "connector"));
        config.setSslConfig(SslConfig.parseCOnfig(parse(context, "sslConfig")));

        RequestHeaders[] heads = parse(context, "defaultHeaders");
        Map<String, String> defaultHeaders = new HashMap<>(heads.length);
        for (RequestHeaders head : heads) {
            String name = parse(head, "name");
            String value = parse(head, "defaultValue");
            defaultHeaders.put(name, value);
        }
        config.setDefaultHeaders(defaultHeaders);

        return config;
    }

    public String getHost() {
        return this.host;
    }

    public String getContextPath() {
        return this.contextPath;
    }

    public long getTimeout() {
        return this.timeout;
    }

    public int getPoolCapacity() {
        return this.poolCapacity;
    }

    public boolean isLazyInit() {
        return this.lazyInit;
    }

    public boolean isShowRequest() {
        return this.showRequest;
    }

    public boolean isShowResponse() {
        return this.showResponse;
    }

    public ConnectorType getConnector() {
        return this.connector;
    }

    public Map<String, String> getDefaultHeaders() {
        if (this.defaultHeaders == null) {
            defaultHeaders = new HashMap<>();
            defaultHeaders.put("Accept", "application/json");
            defaultHeaders.put("Content-Type", "application/json");
            defaultHeaders.put("Connection", "keep-alive");
            defaultHeaders.put("Cache-Control", "no-cache");
        }
        return this.defaultHeaders;
    }

    public SslConfig getSslConfig() {
        return this.sslConfig;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setPoolCapacity(int poolCapacity) {
        this.poolCapacity = poolCapacity;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public void setShowRequest(boolean showRequest) {
        this.showRequest = showRequest;
    }

    public void setShowResponse(boolean showResponse) {
        this.showResponse = showResponse;
    }

    public void setConnector(ConnectorType connector) {
        this.connector = connector;
    }

    public void setDefaultHeaders(Map<String, String> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
    }

    public void setSslConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

}

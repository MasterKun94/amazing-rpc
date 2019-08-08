package httpService.util;

import static httpService.util.AliasUtil.parse;

public class SslConfig {
    private boolean enableOcsp;
    private long sessionCacheSize;
    private long sessionTimeout;
    private String keyStoreType;
    private String keyStoreLocation;
    private String keyStorePassword;
    private String keyPassword;
    private String trustStoreLocation;
    private String trustStorePassword;
    private String[] ciphers;
    private String[] protocols;

    public static SslConfig parseCOnfig(httpService.annotation.SslConfig sslConfig) {
        if (!(boolean) parse(sslConfig, "enabled")) {
            return null;
        }
        SslConfig config = new SslConfig();
        config.setEnableOcsp(parse(sslConfig, "enableOcsp"));
        config.setSessionCacheSize(parse(sslConfig, "sessionCacheSize"));
        config.setSessionTimeout(parse(sslConfig, "sessionTimeout"));
        config.setKeyStoreType(parse(sslConfig, "keyStoreType"));
        config.setKeyStoreLocation(parse(sslConfig, "keyStoreLocation"));
        config.setKeyStorePassword(parse(sslConfig, "keyStorePassword"));
        config.setKeyPassword(parse(sslConfig, "keyPassword"));
        config.setTrustStoreLocation(parse(sslConfig, "trustStoreLocation"));
        config.setTrustStorePassword(parse(sslConfig, "trustStorePassword"));
        config.setCiphers(parse(sslConfig, "ciphers"));
        config.setProtocols(parse(sslConfig, "protocols"));
        return config;
    }

    public boolean isEnableOcsp() {
        return this.enableOcsp;
    }

    public long getSessionCacheSize() {
        return this.sessionCacheSize;
    }

    public long getSessionTimeout() {
        return this.sessionTimeout;
    }

    public String getKeyStoreType() {
        return this.keyStoreType;
    }

    public String getKeyStoreLocation() {
        return this.keyStoreLocation;
    }

    public String getKeyStorePassword() {
        return this.keyStorePassword;
    }

    public String getKeyPassword() {
        return this.keyPassword;
    }

    public String getTrustStoreLocation() {
        return this.trustStoreLocation;
    }

    public String getTrustStorePassword() {
        return this.trustStorePassword;
    }

    public String[] getCiphers() {
        return this.ciphers;
    }

    public String[] getProtocols() {
        return this.protocols;
    }

    public void setEnableOcsp(boolean enableOcsp) {
        this.enableOcsp = enableOcsp;
    }

    public void setSessionCacheSize(long sessionCacheSize) {
        this.sessionCacheSize = sessionCacheSize;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public void setKeyStoreLocation(String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public void setTrustStoreLocation(String trustStoreLocation) {
        this.trustStoreLocation = trustStoreLocation;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public void setCiphers(String[] ciphers) {
        this.ciphers = ciphers;
    }

    public void setProtocols(String[] protocols) {
        this.protocols = protocols;
    }
}

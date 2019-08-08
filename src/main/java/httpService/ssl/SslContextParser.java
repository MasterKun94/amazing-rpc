package httpService.ssl;

import httpService.util.SslConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Arrays;

public class SslContextParser {
    private static final String DEFAULT = "";

    public static SslContext get(SslConfig sslConfig) {
        if (sslConfig == null) {
            return null;
        }
        try {

            String keyStoreType = getKeyStoreType(sslConfig.getKeyStoreType());
            File trustStoreFile = getFile(sslConfig.getTrustStoreLocation());
            TrustManagerFactory trmf;
            if (trustStoreFile == null) {
                trmf = null;
            } else {
                char[] trustStorePswd = getPassword(sslConfig.getTrustStorePassword());
                KeyStore trustStore = KeyStore.getInstance(keyStoreType);
                trustStore.load(new FileInputStream(trustStoreFile), trustStorePswd);
                trmf = TrustManagerFactory.getInstance("SunX509");
                trmf.init(trustStore);
            }

            File keyStoreFile = getFile(sslConfig.getKeyStoreLocation());
            KeyManagerFactory kmf;
            if (keyStoreFile == null) {
                kmf = null;
            } else {
                char[] keyStorePswd = getPassword(sslConfig.getKeyStorePassword());
                char[] keyPswd = getPassword(sslConfig.getKeyPassword());
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(new FileInputStream(keyStoreFile), keyStorePswd);
                kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(keyStore, keyPswd);
            }
            return SslContextBuilder.forClient()
                    .enableOcsp(sslConfig.isEnableOcsp())
                    .sessionCacheSize(sslConfig.getSessionCacheSize())
                    .sessionTimeout(sslConfig.getSessionTimeout())
                    .trustManager(trmf)
                    .keyStoreType(keyStoreType)
                    .keyManager(kmf)
                    .ciphers(sslConfig.getCiphers().length == 0 ?
                            null :
                            Arrays.asList(sslConfig.getCiphers()))
                    .protocols(sslConfig.getProtocols().length == 0 ?
                            null :
                            sslConfig.getProtocols())
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("illegal ssl configuration");
        }
    }

    private static String getKeyStoreType(String pre) {
        return DEFAULT.equals(pre) ? KeyStore.getDefaultType() : "jks";
    }

    private static char[] getPassword(String password) {
        return DEFAULT.equals(password) ? null : password.toCharArray();
    }

    private static File getFile(String path) {
        return DEFAULT.equals(path) ? null : new File(path);
    }
}

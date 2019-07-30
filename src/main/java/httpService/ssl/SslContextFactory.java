package httpService.ssl;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Arrays;

public class SslContextFactory {
    private static final String DEFAULT = "";

    public static SslContext get(SslConfig sslConfig) {
        if (!sslConfig.enabled()) {
            return null;
        }
        try {

            String keyStoreType = getKeyStoreType(sslConfig.keyStoreType());
            File trustStoreFile = getFile(sslConfig.trustStoreLocation());
            TrustManagerFactory trmf;
            if (trustStoreFile == null) {
                trmf = null;
            } else {
                char[] trustStorePswd = getPassword(sslConfig.trustStorePassword());
                KeyStore trustStore = KeyStore.getInstance(keyStoreType);
                trustStore.load(new FileInputStream(trustStoreFile), trustStorePswd);
                trmf = TrustManagerFactory.getInstance("SunX509");
                trmf.init(trustStore);
            }

            File keyStoreFile = getFile(sslConfig.keyStoreLocation());
            KeyManagerFactory kmf;
            if (keyStoreFile == null) {
                kmf = null;
            } else {
                char[] keyStorePswd = getPassword(sslConfig.keyStorePassword());
                char[] keyPswd = getPassword(sslConfig.keyPassword());
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(new FileInputStream(keyStoreFile), keyStorePswd);
                kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(keyStore, keyPswd);
            }
            return SslContextBuilder.forClient()
                    .enableOcsp(sslConfig.enableOcsp())
                    .sessionCacheSize(sslConfig.sessionCacheSize())
                    .sessionTimeout(sslConfig.sessionTimeout())
                    .trustManager(trmf)
                    .keyStoreType(keyStoreType)
                    .keyManager(kmf)
                    .ciphers(sslConfig.ciphers().length == 0 ?
                            null :
                            Arrays.asList(sslConfig.ciphers()))
                    .protocols(sslConfig.protocols().length == 0 ?
                            null :
                            sslConfig.protocols())
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

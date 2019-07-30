package httpService.ssl;

public @interface SslConfig {
    boolean enabled() default false;

    boolean enableOcsp() default false;

    long sessionCacheSize() default 0;

    long sessionTimeout() default 0;

    String keyStoreType() default "";

    String keyStoreLocation() default "";

    String keyStorePassword() default "";

    String keyPassword() default "";

    String trustStoreLocation() default "";

    String trustStorePassword() default "";

    String[] ciphers() default {};

    String[] protocols() default {};
}

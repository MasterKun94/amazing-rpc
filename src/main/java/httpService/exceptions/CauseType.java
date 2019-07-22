package httpService.exceptions;

public enum CauseType {
    CHANNELPOOL_PULL_FAILED(ChannelPoolException.class),

    CHANNELPOOL_PULL_TIMEOUT(ChannelPoolException.class),

    CONNECTION_CONNECT_FAILED(ConnectionException.Failed.class),

    CONNECTION_CONNECT_TIMEOUT(ConnectionException.ConnectTimeout.class),

    CONNECTION_REQUEST_TIMEOUT(ConnectionException.RequestTimeout.class),

    INVALID_RESPONSE(Exception.class),

    DECODE_FAILED(Exception.class),

    SERVER_4$$(ServerException._4$$.class),

    SERVER_5$$(ServerException._5$$.class),

    DEFAULT(Exception.class);

    CauseType(Class<? extends Exception> exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    private Class<? extends Exception> exceptionClass;

    public Class<? extends Exception> getExceptionClass() {
        return exceptionClass;
    }
}

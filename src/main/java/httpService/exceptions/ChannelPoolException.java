package httpService.exceptions;

public class ChannelPoolException extends RpcServiceException {

    private ChannelPoolException(CauseType type) {
        super(type);
    }

    private ChannelPoolException(CauseType type, String message) {
        super(type, message);
    }

    private ChannelPoolException(CauseType type, String message, Throwable cause) {
        super(type, message, cause);
    }

    private ChannelPoolException(CauseType type, Throwable cause) {
        super(type, cause);
    }

    public static class PullTimeout extends ChannelPoolException {

        public PullTimeout() {
            super(CauseType.CHANNELPOOL_PULL_TIMEOUT);
        }

        public PullTimeout(String message) {
            super(CauseType.CHANNELPOOL_PULL_TIMEOUT, message);
        }

        public PullTimeout(String message, Throwable cause) {
            super(CauseType.CHANNELPOOL_PULL_TIMEOUT, message, cause);
        }

        public PullTimeout(Throwable cause) {
            super(CauseType.CHANNELPOOL_PULL_TIMEOUT, cause);
        }
    }
}

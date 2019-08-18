package httpService.util.fallBack;

import httpService.exceptions.CauseType;

public class FallBackInfo {
    private static ThreadLocal<CauseType> causeType = new ThreadLocal<>();
    private static ThreadLocal<Throwable> cause = new ThreadLocal<>();

    public static void setCauseType(CauseType causeType) {
        FallBackInfo.causeType.set(causeType);
    }

    public static void setCause(Throwable cause) {
        FallBackInfo.cause.set(cause);
    }

    public static CauseType getCauseType() {
        return causeType.get();
    }

    public static Throwable getCause() {
        return cause.get();
    }

    public static void reset() {
        causeType.remove();
        cause.remove();
    }
}

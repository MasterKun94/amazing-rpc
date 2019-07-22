package pool.util;

public class Box {
    private static final int EMPTY = -1;
    private static final int SEND_BACK = -2;

    private volatile int payload;

    void setPayload(int payload) {
        this.payload = payload;
    }

    int getPayload() {
        return payload;
    }

    void sendBack() {
        payload = SEND_BACK;
    }

    boolean isSendBack() {
        return payload == SEND_BACK;
    }

    void clean() {
        payload = EMPTY;
    }

    boolean isEmpty() {
        return payload == EMPTY;
    }

    static Box emptyBox() {
        Box box = new Box();
        box.payload = EMPTY;
        return box;
    }
}

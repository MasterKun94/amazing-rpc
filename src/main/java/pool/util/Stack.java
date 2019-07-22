package pool.util;

import java.util.ArrayList;
import java.util.List;

class Stack<T> {
    private List<T> list;
    private int index;

    Stack() {
        this.list = new ArrayList<>();
        this.index = -1;
    }

    void push(T t) {
        list.add(t);
        index++;
    }

    T pop() {
        if (index < 0) {
            throw new IllegalArgumentException();
        } else {
            T t = list.remove(index);
            index--;
            return t;
        }
    }

    T peek() {
        if (index < 0) {
            throw new IllegalArgumentException();
        } else {
            return get(-1);
        }
    }

    T get(int i) {
        if (i < 0) {
            return get(i + index + 1);
        }
        return list.get(i);
    }

    int getIndex() {
        return index;
    }
}

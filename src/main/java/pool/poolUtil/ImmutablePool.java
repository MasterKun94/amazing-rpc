package pool.poolUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

public  class ImmutablePool<T> implements Pool<T> {
    private static final int ELE_SIZE = 4;

    private final T[] elements;
    private final AtomicIntegerArray referenceCounter;
    private final PointerIndexer<T> indexer;

    private final Node root;

    @SuppressWarnings("unchecked")
    ImmutablePool(int capacity, IndexElementInitializer<T> supplier) {
        elements = (T[]) new Object[capacity];
        referenceCounter = new AtomicIntegerArray(capacity);
        Map<T, Integer> map = new TreeMap<>(Comparator.comparingInt(Object::hashCode));
        try {
            T t;
            for (int i = 0; i < capacity; i++) {
                t = supplier.init(i);
                elements[i] = t;
                map.put(t, i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        indexer = new HashPointerIndexer(map);
        Stack<Integer> intStack = new Stack<>();
        Stack<Node> nodeStack = new Stack<>();

        Node node;
        int start;
        int end;
        for (int i = 0; i < capacity; i = i + ELE_SIZE) {
            start = i;
            end = capacity < i + ELE_SIZE ? capacity - 1 : i + ELE_SIZE - 1;
            node = new Node(start, end, null, null);
            node.setAvailableAmount(end - start + 1);
            nodeStack.push(node);
            intStack.push(1);
            refresh(nodeStack, intStack);
        }
        reduce(nodeStack, intStack);
        root = nodeStack.pop();
    }

    ImmutablePool(int capacity, Class<T> clazz) {
        this(capacity, (i) -> clazz.getConstructor().newInstance());
    }

    @Override
    public T borrow() {
        int index = borrowIndex();
        return index == -1 ? null : elements[index];
    }

    @Override
    public int borrowIndex() {
        try {
            return root.getAvailableAmount() == 0 ? -1 : getAvailableIndex(root);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public T request() {
        if (root.getAvailableAmount() == 0) {
            throw new IllegalStateException("Pool full");
        } else {
            T t = getAvailable(root);
            if (t != null) {
                return t;
            } else {
                throw new RuntimeException();
            }
        }
    }

    @Override
    public int addReference(T t) {
        return addReference(getPointer(t));
    }

    @Override
    public int addReference(int pointer) {
        return referenceCounter.incrementAndGet(pointer);
    }

    @Override
    public int release(T t) {
        return release(getPointer(t));
    }

    @Override
    public int release(int pointer) {
        int count = referenceCounter.decrementAndGet(pointer);
        if (count < 0) {
            referenceCounter.incrementAndGet(pointer);
            throw new IllegalArgumentException();//TODO
        } else if (count >= 1) {
            return count;
        } else {
            releaseReference(root, pointer);
            return 0;
        }
    }

    @Override
    public int getCounter(T t) {
        return getCounter(getPointer(t));
    }

    @Override
    public int getCounter(int pointer) {
        return referenceCounter.get(pointer);
    }

    @Override
    public int getPointer(T t) {
        return indexer.index(t);
    }

    @Override
    public T getElement(int pointer) {
        return elements[pointer];
    }

    @Override
    public int availableAmount() {
        return root.getAvailableAmount();
    }

    @Override
    public boolean isFull() {
        return root.getAvailableAmount() == 0;
    }

    @Override
    public int size() {
        return root.getEnd() - root.getStart() + 1;
    }

    private T getAvailable(Node node) {
        int index = getAvailableElementIndex(node);
        return index == -1 ? null : elements[index];
    }

    private int getAvailableIndex(Node node) throws Exception {
        int key = node.decrementAndGetAmount();
        try {
            Node child;
            if (key >= 0) {
                for (int i = 0; i < 3; i++) {
                    Node left = node.getLeft();
                    Node right = node.getRight();
                    child = (key + i) % 2 == 0 ?
                            left.getAvailableAmount() > 0 ?
                                    left :
                                    right :
                            right.getAvailableAmount() > 0 ?
                                    right :
                                    left;

                    if (child != null) {
                        if (child.getLeft() != null) {
                            return getAvailableIndex(child);
                        }
                        if (child.decrementAndGetAmount() >= 0) {
                            int t = getAvailableElementIndex(child);
                            if (t != -1) {
                                return t;
                            }
                        } else {
                            child.incrementAndGetAmount();
                        }
                    }
                }
            }
        } catch (Exception e) {
            node.incrementAndGetAmount();
            throw e;
        }
        node.incrementAndGetAmount();
        return -1;
    }

    private int getAvailableElementIndex(Node node) {
        int start = node.getStart();
        int end = node.getEnd();
        for (int i = start; i <= end; i++) {
            if (referenceCounter.compareAndSet(i, 0, 1)) {
                return i;
            }
        }
        return -1;
    }

    private void refresh(Stack<Node> nodeStack, Stack<Integer> intStack) {
        if (intStack.getIndex() > 0) {
            if (intStack.get(-1).equals(intStack.get(-2))) {
                intStack.pop();
                Integer integer = intStack.pop();
                Node rightNode = nodeStack.pop();
                Node leftNode = nodeStack.pop();

                Node parentNode = new Node(
                        leftNode.getStart(),
                        rightNode.getEnd(),
                        leftNode,
                        rightNode);
                int i = leftNode.getAvailableAmount() + rightNode.getAvailableAmount();
                parentNode.setAvailableAmount(i);
                intStack.push(integer << 1);
                nodeStack.push(parentNode);
                refresh(nodeStack, intStack);
            }
        }
    }

    private void reduce(Stack<Node> nodeStack, Stack<Integer> intStack) {
        if (intStack.getIndex() > 0) {
            Integer integer = intStack.pop() << 1;
            intStack.push(integer);
            refresh(nodeStack, intStack);
            reduce(nodeStack, intStack);
        }
    }

    private void releaseReference(Node node, int pointer) {
        Node left = node.getLeft();
        Node right = node.getRight();

        if (left != null) {
            releaseReference(left.getEnd() >= pointer ? left : right, pointer);
        }
        node.incrementAndGetAmount();
    }

    public class HashPointerIndexer implements PointerIndexer<T> {
        private final int[] hashcodeArray;
        private final int[] pointerArray;

        private HashPointerIndexer(Map<T, Integer> map) {
            List<T> list = new ArrayList<>(map.keySet());
            list.sort(Comparator.comparingInt(Object::hashCode));
            int listSize = list.size();
            hashcodeArray = new int[listSize];
            pointerArray = new int[listSize];
            T o;
            for (int i = 0; i < listSize; i++) {
                o = list.get(i);
                hashcodeArray[i] = o.hashCode();
                pointerArray[i] = map.get(o);
            }
        }

        @Override
        public int index(T object) {

            int reqHash = object.hashCode();
            int minIdx = 0;
            int maxIdx = pointerArray.length - 1;
            int get;
            int idx;
            int minHash = hashcodeArray[minIdx];
            int maxHash = hashcodeArray[maxIdx];
            do {
                idx = ((reqHash - minHash)) / ((maxHash - minHash) / (maxIdx - minIdx)) + minIdx;
                get = hashcodeArray[idx];
                if (reqHash > get) {
                    minIdx = idx + 1;
                    minHash = hashcodeArray[minIdx];
                } else if (reqHash < get) {
                    maxIdx = idx - 1;
                    maxHash = hashcodeArray[maxIdx];
                }
            } while (reqHash != get);

            if (object == getElement(pointerArray[idx])) {
                return pointerArray[idx];
            }
            int idx2 = idx + 1;
            while (reqHash == hashcodeArray[idx2]) {
                if (object == getElement(pointerArray[idx2])) {
                    return idx2;
                }
                idx2++;
            }
            idx2 = idx - 1;
            while (reqHash == hashcodeArray[idx2]) {
                if (object == getElement(pointerArray[idx2])) {
                    return idx2;
                }
                idx2--;
            }
            throw new IllegalArgumentException("该对象不在对象池中");
        }
    }
}

package pool.poolUtil;

import java.util.concurrent.atomic.AtomicInteger;

class Node {
    private final int start;
    private final int end;
    private final Node left;
    private final Node right;

    private final AtomicInteger availableAmount;

    Node(int start, int end, Node left, Node right) {
        this.start = start;
        this.end = end;
        this.left = left;
        this.right = right;
        this.availableAmount = new AtomicInteger();
    }

    Node getLeft() {
        return left;
    }

    Node getRight() {
        return right;
    }

    int getStart() {
        return start;
    }

    int getEnd() {
        return end;
    }

    int getAvailableAmount() {
        return availableAmount.get();
    }

    void setAvailableAmount(int availableAmount) {
        this.availableAmount.set(availableAmount);
    }

    int incrementAndGetAmount() {
        return availableAmount.incrementAndGet();
    }

    int decrementAndGetAmount() {
        return availableAmount.decrementAndGet();
    }

//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < layer; i++) {
//            sb.append("\t");
//        }
//        String tab = sb.toString();
//        return "{\n" + tab + "\t\"start\" : " + this.getStart() +
//                ", \n" + tab + "\t\"end\" : " + this.getEnd() +
//                ", \n" + tab + "\t\"layer\" : " + this.getLayer() +
//                ", \n" + tab + "\t\"availableAmount\" : " + this.getAvailableAmount() +
//                ", \n" + tab + "\t\"left\" : " + this.getLeft() +
//                ", \n" + tab + "\t\"right\" : " + this.getRight() +
//                "\n" + tab + "}";
//    }
}

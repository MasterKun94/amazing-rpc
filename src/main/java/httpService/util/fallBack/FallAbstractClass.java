package httpService.util.fallBack;

public abstract class FallAbstractClass<IN1, IN2, IN3, IN4, IN5, OUT> {
    public FallAbstractClass(FallBackMethod<IN1, IN2, IN3, IN4, IN5, OUT> fallBackMethod) {

    }

    public abstract OUT fallBack(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5);

    public static void main(String[] args) {
        Test test = new Test() {
            @Override
            public Object fallBack(int in1, int in2, int in3, int in4, int in5) {
                return null;
            }
        };
        new FallAbstractClass<Integer, Integer, Integer, Integer, Integer, Object>(test::fallBack) {
            @Override
            public Object fallBack(Integer o, Integer o2, Integer o3, Integer o4, Integer o5) {
                return null;
            }
        };
    }
}

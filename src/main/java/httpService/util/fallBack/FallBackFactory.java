package httpService.util.fallBack;

public class FallBackFactory {
    public static  <IN1, IN2, IN3, IN4, IN5, OUT> FallBackFactory createFor(FallBackMethod<IN1, IN2, IN3, IN4, IN5, OUT> fallBackMethod) {
        return null;
    }

    public static void main(String[] args) {
        Test test = new Test() {
            @Override
            public Object fallBack(int in1, int in2, int in3, int in4, int in5) {
                return null;
            }
        };
        FallBackFactory aFor = FallBackFactory.createFor(test::fallBack);
    }
}

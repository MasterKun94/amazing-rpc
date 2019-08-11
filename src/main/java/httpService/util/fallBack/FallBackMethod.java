package httpService.util.fallBack;

public interface FallBackMethod<IN1, IN2, IN3, IN4, IN5, OUT> {
    OUT fallBack(IN1 in1, IN2 in2, IN3 in3, IN4 in4, IN5 in5);
}

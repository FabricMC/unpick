package pkg;

import java.util.function.IntConsumer;

public class TestLambdaInsideToOutsideCapture {
	private static void test() {
		int n = Constants.INT_CONST_1;
		IntConsumer ic = i -> Constants.consumeInt(n);
	}
}

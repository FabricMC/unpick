package pkg;

import java.util.function.IntUnaryOperator;

public class TestLambdaOutsideToInsideCapture {
	private static int supplyInt() {
		return 0;
	}

	private static void test() {
		int n = supplyInt();
		IntUnaryOperator iuo = i -> n == Constants.INT_CONST_1 ? 0 : 1;
	}
}

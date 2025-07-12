package pkg;

public class TestLambdaGroupedFunctionalInterfaceReturn {
	void test() {
		I itf = () -> Constants.INT_CONST_1;
	}

	@FunctionalInterface
	interface I {
		int run();
	}
}

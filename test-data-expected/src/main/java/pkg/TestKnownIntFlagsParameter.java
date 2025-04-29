package pkg;

public class TestKnownIntFlagsParameter {
	public void test1() {
		Constants.consumeInt(Constants.INT_FLAG_BIT_2);
	}

	public void test2() {
		Constants.consumeInt(Constants.INT_FLAG_BIT_2 | Constants.INT_FLAG_BIT_3);
	}

	public void test3() {
		Constants.consumeInt(Constants.INT_FLAG_BIT_1 | Constants.INT_FLAG_BIT_3);
	}

	public void test4() {
		Constants.consumeInt(Constants.INT_FLAG_BIT_0 | Constants.INT_FLAG_BIT_1 | Constants.INT_FLAG_BIT_2);
	}
}

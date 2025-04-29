package pkg;

public class TestNegatedLongFlagsParameter {
	public void test1() {
		Constants.consumeLong(~Constants.LONG_FLAG_BIT_2);
	}

	public void test2() {
		Constants.consumeLong(~(Constants.LONG_FLAG_BIT_2 | Constants.LONG_FLAG_BIT_3));
	}

	public void test3() {
		Constants.consumeLong(~(Constants.LONG_FLAG_BIT_1 | Constants.LONG_FLAG_BIT_3));
	}

	public void test4() {
		Constants.consumeLong(~(Constants.LONG_FLAG_BIT_0 | Constants.LONG_FLAG_BIT_1 | Constants.LONG_FLAG_BIT_2));
	}
}

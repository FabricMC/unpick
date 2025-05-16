package pkg;

public class TestKnownDoubleConstantsParameter {
	public void test1() {
		Constants.consumeDouble(Constants.DOUBLE_CONST_0);
	}

	public void test2() {
		Constants.consumeDouble(Constants.DOUBLE_CONST_1);
	}

	public void test3() {
		Constants.consumeDouble(Constants.DOUBLE_CONST);
	}
}

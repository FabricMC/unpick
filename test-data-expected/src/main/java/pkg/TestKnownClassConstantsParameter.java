package pkg;

public class TestKnownClassConstantsParameter {
	public void test1() {
		Constants.consumeClass(Constants.CLASS_CONST_STRING);
	}

	public void test2() {
		Constants.consumeClass(Constants.CLASS_CONST_INTEGER);
	}

	public void test3() {
		Constants.consumeClass(Constants.CLASS_CONST_NULL);
	}
}

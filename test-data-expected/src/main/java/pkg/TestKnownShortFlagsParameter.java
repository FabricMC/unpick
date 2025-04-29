package pkg;

public class TestKnownShortFlagsParameter {
	public void test1() {
		Constants.consumeShort(Constants.SHORT_FLAG_BIT_2);
	}

	public void test2() {
		Constants.consumeShort((short) (Constants.SHORT_FLAG_BIT_2 | Constants.SHORT_FLAG_BIT_3));
	}

	public void test3() {
		Constants.consumeShort((short) (Constants.SHORT_FLAG_BIT_1 | Constants.SHORT_FLAG_BIT_3));
	}

	public void test4() {
		Constants.consumeShort((short) (Constants.SHORT_FLAG_BIT_0 | Constants.SHORT_FLAG_BIT_1 | Constants.SHORT_FLAG_BIT_2));
	}
}

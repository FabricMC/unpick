package pkg;

public class TestKnownByteFlagsParameter {
	public void test1() {
		Constants.consumeByte(Constants.BYTE_FLAG_BIT_2);
	}

	public void test2() {
		Constants.consumeByte((byte) (Constants.BYTE_FLAG_BIT_2 | Constants.BYTE_FLAG_BIT_3));
	}

	public void test3() {
		Constants.consumeByte((byte) (Constants.BYTE_FLAG_BIT_1 | Constants.BYTE_FLAG_BIT_3));
	}

	public void test4() {
		Constants.consumeByte((byte) (Constants.BYTE_FLAG_BIT_0 | Constants.BYTE_FLAG_BIT_1 | Constants.BYTE_FLAG_BIT_2));
	}
}

package pkg;

public class TestKnownShortFlagsReturn {
	public short test1() {
		return Constants.SHORT_FLAG_BIT_2;
	}

	public short test2() {
		return (short) (Constants.SHORT_FLAG_BIT_2 | Constants.SHORT_FLAG_BIT_3);
	}

	public short test3() {
		return (short) (Constants.SHORT_FLAG_BIT_1 | Constants.SHORT_FLAG_BIT_3);
	}

	public short test4() {
		return (short) (Constants.SHORT_FLAG_BIT_0 | Constants.SHORT_FLAG_BIT_1 | Constants.SHORT_FLAG_BIT_2);
	}
}

package pkg;

public class TestExpressions {
	public int testAdd() {
		return Constants.INT_CONST_5 + 1;
	}

	public int testSub() {
		return Constants.INT_CONST_M1 - 1;
	}

	public int testMul() {
		return Constants.INT_CONST_5 * 2;
	}

	public int testDiv() {
		return Constants.INT_CONST_5 / 2;
	}

	public int testMod() {
		return Constants.INT_CONST_5 % 2;
	}

	public int testShiftLeft() {
		return Constants.INT_CONST_5 << 2;
	}

	public int testShiftRight() {
		return Constants.INT_CONST_5 >> 3;
	}

	public int testShiftRightUnsigned() {
		return Constants.INT_CONST >>> 1;
	}

	public int testNeg() {
		return -Constants.INT_CONST_5;
	}

	public int testBitInverse() {
		return ~Constants.INT_CONST_5;
	}

	public int testIntCast() {
		return (int) Constants.LONG_CONST;
	}

	public long testAddLong() {
		return Constants.LONG_CONST_1 + 4;
	}

	public long testSubLong() {
		return Constants.LONG_CONST_0 - 3;
	}

	public long testMulLong() {
		return Constants.LONG_CONST_1 * 10;
	}

	public long testDivLong() {
		return Constants.LONG_CONST / 10;
	}

	public long testModLong() {
		return Constants.LONG_CONST % 127;
	}

	public long testShiftLeftLong() {
		return Constants.LONG_CONST_1 << 3;
	}

	public long testShiftRightLong() {
		return Constants.LONG_CONST_1 >> 1;
	}

	public long testShiftRightUnsignedLong() {
		return Constants.LONG_CONST >>> 1;
	}

	public long testNegLong() {
		return -Constants.LONG_CONST_1;
	}

	public long testBitInverseLong() {
		return ~Constants.LONG_CONST_1;
	}

	public float testAddFloat() {
		return Constants.FLOAT_CONST_1 + 2;
	}

	public float testSubFloat() {
		return Constants.FLOAT_CONST_0 - 2;
	}

	public float testMulFloat() {
		return Constants.FLOAT_CONST_1 * 2;
	}

	public float testDivFloat() {
		return Constants.FLOAT_CONST_1 / 2;
	}

	public float testModFloat() {
		return Constants.FLOAT_CONST_1 % 2;
	}

	public float testNegFloat() {
		return -Constants.FLOAT_CONST_1;
	}

	public double testAddDouble() {
		return Constants.DOUBLE_CONST_1 + 2;
	}

	public double testSubDouble() {
		return Constants.DOUBLE_CONST_0 - 2;
	}

	public double testMulDouble() {
		return Constants.DOUBLE_CONST_1 * 2;
	}

	public double testDivDouble() {
		return Constants.DOUBLE_CONST_1 / 2;
	}

	public double testModDouble() {
		return Constants.DOUBLE_CONST_1 % 2;
	}

	public double testNegDouble() {
		return -Constants.DOUBLE_CONST_1;
	}
}

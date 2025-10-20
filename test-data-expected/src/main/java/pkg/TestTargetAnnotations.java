package pkg;

public class TestTargetAnnotations {
	@TestAnnotation
	int field;

	boolean testField() {
		return field == Constants.INT_CONST_1;
	}

	@TestAnnotation
	int testReturn() {
		return Constants.INT_CONST_1;
	}

	boolean testParam(@TestAnnotation int i) {
		return i == Constants.INT_CONST_1;
	}

	boolean testReturnRef() {
		return testReturn() == Constants.INT_CONST_1;
	}

	void testParamRef() {
		testParam(Constants.INT_CONST_1);
	}
}

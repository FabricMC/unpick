package pkg;

public class TestTargetTypeAnnotations {
	@TestTypeAnnotation
	int field;

	boolean testField() {
		return field == Constants.INT_CONST_1;
	}

	@TestTypeAnnotation
	int testReturn() {
		return Constants.INT_CONST_1;
	}

	boolean testParam(@TestTypeAnnotation int i) {
		return i == Constants.INT_CONST_1;
	}

	boolean testReturnRef() {
		return testReturn() == Constants.INT_CONST_1;
	}

	void testParamRef() {
		testParam(Constants.INT_CONST_1);
	}
}

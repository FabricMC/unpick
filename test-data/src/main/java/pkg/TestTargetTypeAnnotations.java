package pkg;

public class TestTargetTypeAnnotations {
	@TestTypeAnnotation
	int field;

	boolean testField() {
		return field == 1;
	}

	@TestTypeAnnotation
	int testReturn() {
		return 1;
	}

	boolean testParam(@TestTypeAnnotation int i) {
		return i == 1;
	}

	boolean testReturnRef() {
		return testReturn() == 1;
	}

	void testParamRef() {
		testParam(1);
	}
}

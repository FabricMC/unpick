package daomephsta.unpick.tests;

import org.junit.jupiter.api.Test;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

public class TestGlobalConstants {
	@Test
	public void testGlobalStringConstant() {
		TestUtils.runTest("pkg/TestKnownStringConstantsReturn", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.STRING)
					.constant(new FieldExpression("pkg.Constants", "STRING_CONST_FOO", null, true))
					.constant(new FieldExpression("pkg.Constants", "STRING_CONST_BAR", null, true))
					.constant(new FieldExpression("pkg.Constants", "STRING_CONST_NULL", null, true))
					.build());
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.CLASS)
					.constant(new FieldExpression("pkg.Constants", "CLASS_CONST_NULL", null, true))
					.build());
		});
	}

	@Test
	public void testGlobalClassConstant() {
		TestUtils.runTest("pkg/TestKnownClassConstantsReturn", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.CLASS)
					.constant(new FieldExpression("pkg.Constants", "CLASS_CONST_STRING", null, true))
					.constant(new FieldExpression("pkg.Constants", "CLASS_CONST_INTEGER", null, true))
					.constant(new FieldExpression("pkg.Constants", "CLASS_CONST_NULL", null, true))
					.build());
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.STRING)
					.constant(new FieldExpression("pkg.Constants", "STRING_CONST_NULL", null, true))
					.build());
		});
	}
}

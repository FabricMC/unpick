package daomephsta.unpick.tests;

import org.junit.jupiter.api.Test;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

public class TestSubclass {
	@Test
	public void testSubclass() {
		TestUtils.runTest("pkg/TestSubclass$Subclass", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "group")
					.constant(new FieldExpression("pkg.Constants", "INT_CONST", null, true))
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.TestSubclass", "test", "(I)V")
					.paramGroup(0, "group")
					.build());
		});
	}
}

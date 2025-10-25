package daomephsta.unpick.tests;

import org.junit.jupiter.api.Test;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetAnnotation;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

public class TestTargetAnnotations {
	@Test
	public void testTargetAnnotations() {
		TestUtils.runTest("pkg/TestTargetAnnotations", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "g")
					.constant(new FieldExpression("pkg.Constants", "INT_CONST_1", null, true))
					.build());
			data.visitTargetAnnotation(new TargetAnnotation("pkg.TestAnnotation", "g"));
		});
	}
}

package daomephsta.unpick.tests;

import org.junit.jupiter.api.Test;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

public class TestNoSelfReferencing {
	@Test
	public void testNoSelfReferencing() {
		TestUtils.runTest("pkg/TestConstantDeclaration", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.CLASS)
					.constant(new FieldExpression("pkg.TestConstantDeclaration", "FOO", null, true))
					.build());
		});
	}
}

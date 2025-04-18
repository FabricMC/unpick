package daomephsta.unpick.tests;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class TestSubclass
{
	@Test
	public void testSubclass()
	{
		TestUtils.runTest("pkg/TestSubclass$Subclass", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "group")
					.constant(new GroupConstant(new Literal.Long(257), new FieldExpression("pkg.Constants", "INT_CONST", null, true)))
					.build());
			Map<Integer, String> paramGroups = new HashMap<>();
			paramGroups.put(0, "group");
			data.visitTargetMethod(new TargetMethod("pkg.TestSubclass", "test", "(I)V", paramGroups, null));
		});
	}
}

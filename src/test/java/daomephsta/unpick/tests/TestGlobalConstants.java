package daomephsta.unpick.tests;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

import org.junit.jupiter.api.Test;

public class TestGlobalConstants
{
	@Test
	public void testGlobalStringConstant()
	{
		TestUtils.runTest("pkg/TestKnownStringConstantsReturn", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.STRING)
					.constant(new GroupConstant(new Literal.String("foo"), new FieldExpression("pkg.Constants", "STRING_CONST_FOO", null, true)))
					.constant(new GroupConstant(new Literal.String("bar"), new FieldExpression("pkg.Constants", "STRING_CONST_BAR", null, true)))
					.constant(new GroupConstant(Literal.Null.INSTANCE, new FieldExpression("pkg.Constants", "STRING_CONST_NULL", null, true)))
					.build());
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.CLASS)
					.constant(new GroupConstant(Literal.Null.INSTANCE, new FieldExpression("pkg.Constants", "CLASS_CONST_NULL", null, true)))
					.build());
		});
	}

	@Test
	public void testGlobalClassConstant()
	{
		TestUtils.runTest("pkg/TestKnownClassConstantsReturn", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.CLASS)
					.constant(new GroupConstant(new Literal.Class("Ljava/lang/String;"), new FieldExpression("pkg.Constants", "CLASS_CONST_STRING", null, true)))
					.constant(new GroupConstant(new Literal.Class("Ljava/lang/Integer;"), new FieldExpression("pkg.Constants", "CLASS_CONST_INTEGER", null, true)))
					.constant(new GroupConstant(Literal.Null.INSTANCE, new FieldExpression("pkg.Constants", "CLASS_CONST_NULL", null, true)))
					.build());
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.STRING)
					.constant(new GroupConstant(Literal.Null.INSTANCE, new FieldExpression("pkg.Constants", "STRING_CONST_NULL", null, true)))
					.build());
		});
	}
}

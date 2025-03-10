package daomephsta.unpick.tests;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;
import org.junit.jupiter.api.Test;

public class SimpleConstantUninliningTest
{
	@Test
	public void testKnownByteConstantsReturn()
	{
		testConstantReturn("pkg.TestKnownByteConstantsReturn", "byteConsts", "B", 8);
	}

	@Test
	public void testKnownByteConstantsParameter()
	{
		testConstantParameter("pkg.TestKnownByteConstantsParameter");
	}

	@Test
	public void testKnownShortConstantsReturn()
	{
		testConstantReturn("pkg.TestKnownShortConstantsReturn", "shortConsts", "S", 8);
	}

	@Test
	public void testKnownShortConstantsParameter()
	{
		testConstantParameter("pkg.TestKnownShortConstantsParameter");
	}

	@Test
	public void testKnownCharConstantsReturn()
	{
		testConstantReturn("pkg.TestKnownCharConstantsReturn", "charConsts", "C", 7);
	}

	@Test
	public void testKnownCharConstantsParameter()
	{
		testConstantParameter("pkg.TestKnownCharConstantsParameter");
	}

	@Test
	public void testKnownIntConstantsReturn()
	{
		testConstantReturn("pkg.TestKnownIntConstantsReturn", "intConsts", "I", 8);
	}

	@Test
	public void testKnownIntConstantsParameter()
	{
		testConstantParameter("pkg.TestKnownIntConstantsParameter");
	}

	@Test
	public void testKnownLongConstantsReturn()
	{
		testConstantReturn("pkg.TestKnownLongConstantsReturn", "longConsts", "J", 3);
	}

	@Test
	public void testKnownLongConstantsParameter()
	{
		testConstantParameter("pkg.TestKnownLongConstantsParameter");
	}

	@Test
	public void testKnownFloatConstantsReturn()
	{
		testConstantReturn("pkg.TestKnownFloatConstantsReturn", "floatConsts", "F", 4);
	}

	@Test
	public void testKnownFloatConstantsParameter()
	{
		testConstantParameter("pkg.TestKnownFloatConstantsParameter");
	}

	@Test
	public void testKnownDoubleConstantsReturn()
	{
		testConstantReturn("pkg.TestKnownDoubleConstantsReturn", "doubleConsts", "D", 3);
	}

	@Test
	public void testKnownDoubleConstantsParameter()
	{
		testConstantParameter("pkg.TestKnownDoubleConstantsParameter");
	}

	@Test
	public void testKnownStringConstantsReturn()
	{
		testConstantReturn("pkg.TestKnownStringConstantsReturn", "stringConsts", "Ljava/lang/String;", 2);
	}

	@Test
	public void testKnownStringConstantsParameter()
	{
		testConstantParameter("pkg.TestKnownStringConstantsParameter");
	}

	@Test
	public void testUnknownConstants()
	{
		testConstantParameter("pkg.TestUnknownConstants");
	}

	private static void testConstantReturn(String className, String groupName, String descriptor, int methodCount)
	{
		TestUtils.runTest(className.replace('.', '/'), data -> {
			visitGroups(data);
			for (int i = 1; i <= methodCount; i++)
			{
				data.visitTargetMethod(TargetMethod.Builder.builder(className, "test" + i, "()" + descriptor).returnGroup(groupName).build());
			}
		});
	}

	private static void testConstantParameter(String className)
	{
		TestUtils.runTest(className.replace('.', '/'), data -> {
			visitGroups(data);
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeByte", "(B)V").paramGroup(0, "byteConsts").build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeShort", "(S)V").paramGroup(0, "shortConsts").build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeChar", "(C)V").paramGroup(0, "charConsts").build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeInt", "(I)V").paramGroup(0, "intConsts").build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeLong", "(J)V").paramGroup(0, "longConsts").build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeFloat", "(F)V").paramGroup(0, "floatConsts").build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeDouble", "(D)V").paramGroup(0, "doubleConsts").build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeString", "(Ljava/lang/String;)V").paramGroup(0, "stringConsts").build());
		});
	}

	private static void visitGroups(UnpickV3Visitor data)
	{
		data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "byteConsts")
				.constant(new GroupConstant(new Literal.Long(-1), new FieldExpression("pkg.Constants", "BYTE_CONST_M1", DataType.BYTE, true)))
				.constant(new GroupConstant(new Literal.Long(0), new FieldExpression("pkg.Constants", "BYTE_CONST_0", DataType.BYTE, true)))
				.constant(new GroupConstant(new Literal.Long(1), new FieldExpression("pkg.Constants", "BYTE_CONST_1", DataType.BYTE, true)))
				.constant(new GroupConstant(new Literal.Long(2), new FieldExpression("pkg.Constants", "BYTE_CONST_2", DataType.BYTE, true)))
				.constant(new GroupConstant(new Literal.Long(3), new FieldExpression("pkg.Constants", "BYTE_CONST_3", DataType.BYTE, true)))
				.constant(new GroupConstant(new Literal.Long(4), new FieldExpression("pkg.Constants", "BYTE_CONST_4", DataType.BYTE, true)))
				.constant(new GroupConstant(new Literal.Long(5), new FieldExpression("pkg.Constants", "BYTE_CONST_5", DataType.BYTE, true)))
				.constant(new GroupConstant(new Literal.Long(117), new FieldExpression("pkg.Constants", "BYTE_CONST", DataType.BYTE, true)))
				.build());
		data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "shortConsts")
				.constant(new GroupConstant(new Literal.Long(-1), new FieldExpression("pkg.Constants", "SHORT_CONST_M1", DataType.SHORT, true)))
				.constant(new GroupConstant(new Literal.Long(0), new FieldExpression("pkg.Constants", "SHORT_CONST_0", DataType.SHORT, true)))
				.constant(new GroupConstant(new Literal.Long(1), new FieldExpression("pkg.Constants", "SHORT_CONST_1", DataType.SHORT, true)))
				.constant(new GroupConstant(new Literal.Long(2), new FieldExpression("pkg.Constants", "SHORT_CONST_2", DataType.SHORT, true)))
				.constant(new GroupConstant(new Literal.Long(3), new FieldExpression("pkg.Constants", "SHORT_CONST_3", DataType.SHORT, true)))
				.constant(new GroupConstant(new Literal.Long(4), new FieldExpression("pkg.Constants", "SHORT_CONST_4", DataType.SHORT, true)))
				.constant(new GroupConstant(new Literal.Long(5), new FieldExpression("pkg.Constants", "SHORT_CONST_5", DataType.SHORT, true)))
				.constant(new GroupConstant(new Literal.Long(257), new FieldExpression("pkg.Constants", "SHORT_CONST", DataType.SHORT, true)))
				.build());
		data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "charConsts")
				.constant(new GroupConstant(new Literal.Long('\0'), new FieldExpression("pkg.Constants", "CHAR_CONST_0", DataType.CHAR, true)))
				.constant(new GroupConstant(new Literal.Long('\1'), new FieldExpression("pkg.Constants", "CHAR_CONST_1", DataType.CHAR, true)))
				.constant(new GroupConstant(new Literal.Long('\2'), new FieldExpression("pkg.Constants", "CHAR_CONST_2", DataType.CHAR, true)))
				.constant(new GroupConstant(new Literal.Long('\3'), new FieldExpression("pkg.Constants", "CHAR_CONST_3", DataType.CHAR, true)))
				.constant(new GroupConstant(new Literal.Long('\4'), new FieldExpression("pkg.Constants", "CHAR_CONST_4", DataType.CHAR, true)))
				.constant(new GroupConstant(new Literal.Long('\5'), new FieldExpression("pkg.Constants", "CHAR_CONST_5", DataType.CHAR, true)))
				.constant(new GroupConstant(new Literal.Long('\257'), new FieldExpression("pkg.Constants", "CHAR_CONST", DataType.CHAR, true)))
				.build());
		data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "intConsts")
				.constant(new GroupConstant(new Literal.Long(-1), new FieldExpression("pkg.Constants", "INT_CONST_M1", null, true)))
				.constant(new GroupConstant(new Literal.Long(0), new FieldExpression("pkg.Constants", "INT_CONST_0", null, true)))
				.constant(new GroupConstant(new Literal.Long(1), new FieldExpression("pkg.Constants", "INT_CONST_1", null, true)))
				.constant(new GroupConstant(new Literal.Long(2), new FieldExpression("pkg.Constants", "INT_CONST_2", null, true)))
				.constant(new GroupConstant(new Literal.Long(3), new FieldExpression("pkg.Constants", "INT_CONST_3", null, true)))
				.constant(new GroupConstant(new Literal.Long(4), new FieldExpression("pkg.Constants", "INT_CONST_4", null, true)))
				.constant(new GroupConstant(new Literal.Long(5), new FieldExpression("pkg.Constants", "INT_CONST_5", null, true)))
				.constant(new GroupConstant(new Literal.Long(257), new FieldExpression("pkg.Constants", "INT_CONST", null, true)))
				.build());
		data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.LONG, "longConsts")
				.constant(new GroupConstant(new Literal.Long(0), new FieldExpression("pkg.Constants", "LONG_CONST_0", null, true)))
				.constant(new GroupConstant(new Literal.Long(1), new FieldExpression("pkg.Constants", "LONG_CONST_1", null, true)))
				.constant(new GroupConstant(new Literal.Long(1234567890), new FieldExpression("pkg.Constants", "LONG_CONST", null, true)))
				.build());
		data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.FLOAT, "floatConsts")
				.constant(new GroupConstant(new Literal.Double(0), new FieldExpression("pkg.Constants", "FLOAT_CONST_0", null, true)))
				.constant(new GroupConstant(new Literal.Double(1), new FieldExpression("pkg.Constants", "FLOAT_CONST_1", null, true)))
				.constant(new GroupConstant(new Literal.Double(2), new FieldExpression("pkg.Constants", "FLOAT_CONST_2", null, true)))
				.constant(new GroupConstant(new Literal.Double(5.3), new FieldExpression("pkg.Constants", "FLOAT_CONST", null, true)))
				.build());
		data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.DOUBLE, "doubleConsts")
				.constant(new GroupConstant(new Literal.Double(0), new FieldExpression("pkg.Constants", "DOUBLE_CONST_0", null, true)))
				.constant(new GroupConstant(new Literal.Double(1), new FieldExpression("pkg.Constants", "DOUBLE_CONST_1", null, true)))
				.constant(new GroupConstant(new Literal.Double(5.3), new FieldExpression("pkg.Constants", "DOUBLE_CONST", null, true)))
				.build());
		data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.STRING, "stringConsts")
				.constant(new GroupConstant(new Literal.String("foo"), new FieldExpression("pkg.Constants", "STRING_CONST_FOO", null, true)))
				.constant(new GroupConstant(new Literal.String("bar"), new FieldExpression("pkg.Constants", "STRING_CONST_BAR", null, true)))
				.build());
	}
}

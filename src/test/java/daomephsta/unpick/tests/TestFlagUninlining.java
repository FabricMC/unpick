package daomephsta.unpick.tests;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupType;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

import org.junit.jupiter.api.Test;

public class TestFlagUninlining
{
	@Test
	public void testKnownByteFlagsReturn()
	{
		testFlagsReturn("pkg.TestKnownByteFlagsReturn", DataType.BYTE, DataType.INT, "B");
	}

	@Test
	public void testKnownByteFlagsParameter()
	{
		testFlagsParameter("pkg.TestKnownByteFlagsParameter");
	}

	@Test
	public void testKnownShortFlagsReturn()
	{
		testFlagsReturn("pkg.TestKnownShortFlagsReturn",  DataType.SHORT, DataType.INT, "S");
	}

	@Test
	public void testKnownShortFlagsParameter()
	{
		testFlagsParameter("pkg.TestKnownShortFlagsParameter");
	}

	@Test
	public void testKnownIntFlagsReturn()
	{
		testFlagsReturn("pkg.TestKnownIntFlagsReturn", DataType.INT, DataType.INT, "I");
	}

	@Test
	public void testKnownIntFlagsParameter()
	{
		testFlagsParameter("pkg.TestKnownIntFlagsParameter");
	}

	@Test
	public void testKnownLongFlagsReturn()
	{
		testFlagsReturn("pkg.TestKnownLongFlagsReturn", DataType.LONG, DataType.LONG, "J");
	}

	@Test
	public void testKnownLongFlagsParameter()
	{
		testFlagsParameter("pkg.TestKnownLongFlagsParameter");
	}

	@Test
	public void testNegatedByteFlagsReturn()
	{
		testFlagsReturn("pkg.TestNegatedByteFlagsReturn", DataType.BYTE, DataType.INT, "B");
	}

	@Test
	public void testNegatedByteFlagsParameter()
	{
		testFlagsParameter("pkg.TestNegatedByteFlagsParameter");
	}

	@Test
	public void testNegatedShortFlagsReturn()
	{
		testFlagsReturn("pkg.TestNegatedShortFlagsReturn",  DataType.SHORT, DataType.INT, "S");
	}

	@Test
	public void testNegatedShortFlagsParameter()
	{
		testFlagsParameter("pkg.TestNegatedShortFlagsParameter");
	}

	@Test
	public void testNegatedIntFlagsReturn()
	{
		testFlagsReturn("pkg.TestNegatedIntFlagsReturn", DataType.INT, DataType.INT, "I");
	}

	@Test
	public void testNegatedIntFlagsParameter()
	{
		testFlagsParameter("pkg.TestNegatedIntFlagsParameter");
	}

	@Test
	public void testNegatedLongFlagsReturn()
	{
		testFlagsReturn("pkg.TestNegatedLongFlagsReturn", DataType.LONG, DataType.LONG, "J");
	}

	@Test
	public void testNegatedLongFlagsParameter()
	{
		testFlagsParameter("pkg.TestNegatedLongFlagsParameter");
	}

	@Test
	public void testUnknownFlags()
	{
		testFlagsParameter("pkg.TestUnknownFlags");
	}

	@Test
	public void testZeroFlags()
	{
		testFlagsParameter("pkg.TestZeroFlags");
	}

	@Test
	public void testMinusOneFlags()
	{
		testFlagsParameter("pkg.TestMinusOneFlags");
	}

	@Test
	public void testSomeUnknownFlags()
	{
		testFlagsParameter("pkg.TestSomeUnknownFlags");
	}

	private static void testFlagsReturn(String className, DataType dataType, DataType groupDataType, String descriptor)
	{
		TestUtils.runTest(className.replace('.', '/'), data ->
		{
			data.visitGroupDefinition(createFlagsGroup("flags", dataType, groupDataType));
			for (int i = 1; i <= 4; i++)
			{
				data.visitTargetMethod(TargetMethod.Builder.builder(className, "test" + i, "()" + descriptor)
						.returnGroup("flags")
						.build()
				);
			}
		});
	}

	private static void testFlagsParameter(String className)
	{
		TestUtils.runTest(className.replace('.', '/'), data ->
		{
			data.visitGroupDefinition(createFlagsGroup("byteFlags", DataType.BYTE, DataType.INT));
			data.visitGroupDefinition(createFlagsGroup("shortFlags", DataType.SHORT, DataType.INT));
			data.visitGroupDefinition(createFlagsGroup("intFlags", DataType.INT, DataType.INT));
			data.visitGroupDefinition(createFlagsGroup("longFlags", DataType.LONG, DataType.LONG));
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeByte", "(B)V")
					.paramGroup(0, "byteFlags")
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeShort", "(S)V")
					.paramGroup(0, "shortFlags")
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeInt", "(I)V")
					.paramGroup(0, "intFlags")
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeLong", "(J)V")
					.paramGroup(0, "longFlags")
					.build());
		});
	}

	private static GroupDefinition createFlagsGroup(String name, DataType dataType, DataType groupDataType)
	{
		return GroupDefinition.Builder.named(groupDataType, name)
				.type(GroupType.FLAG)
				.constant(new GroupConstant(new Literal.Long(0b0001), new FieldExpression("pkg.Constants", dataType + "_FLAG_BIT_0", dataType, true)))
				.constant(new GroupConstant(new Literal.Long(0b0010), new FieldExpression("pkg.Constants", dataType + "_FLAG_BIT_1", dataType, true)))
				.constant(new GroupConstant(new Literal.Long(0b0100), new FieldExpression("pkg.Constants", dataType + "_FLAG_BIT_2", dataType, true)))
				.constant(new GroupConstant(new Literal.Long(0b1000), new FieldExpression("pkg.Constants", dataType + "_FLAG_BIT_3", dataType, true)))
				.constant(new GroupConstant(new Literal.Long(0), new FieldExpression("pkg.Constants", dataType + "_FLAG_0", dataType, true)))
				.constant(new GroupConstant(new Literal.Long(-1), new FieldExpression("pkg.Constants", dataType + "_FLAG_M1", dataType, true)))
				.build();
	}
}

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

public class TestCoercion
{
	@Test
	public void testFloatToDoubleCoercion()
	{
		TestUtils.runTest("pkg/TestFloatToDoubleCoercion", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.FLOAT)
					.constant(new GroupConstant(new Literal.Double(1), new FieldExpression("pkg.Constants", "FLOAT_CONST_1", null, true)))
					.build());
		});
	}

	@Test
	public void testFloatToDoubleImplicitCast()
	{
		TestUtils.runTest("pkg/TestFloatToDoubleCoercion", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.DOUBLE)
					.constant(new GroupConstant(new Literal.Double(1), new FieldExpression("pkg.Constants", "FLOAT_CONST_1", DataType.FLOAT, true)))
					.build());
		});
	}

	@Test
	public void testByteToIntImplicitCast()
	{
		TestUtils.runTest("pkg/TestByteToIntCoercion", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.INT)
					.constant(new GroupConstant(new Literal.Long(3), new FieldExpression("pkg.Constants", "BYTE_CONST_3", DataType.BYTE, true)))
					.build());
		});
	}

	@Test
	public void testByteToIntFlagImplicitCast()
	{
		TestUtils.runTest("pkg/TestByteToIntFlagCoercion", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "group")
					.type(GroupType.FLAG)
					.constant(new GroupConstant(new Literal.Long(1), new FieldExpression("pkg.Constants", "BYTE_FLAG_BIT_0", DataType.BYTE, true)))
					.constant(new GroupConstant(new Literal.Long(2), new FieldExpression("pkg.Constants", "BYTE_FLAG_BIT_1", DataType.BYTE, true)))
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.TestByteToIntFlagCoercion", "test1", "()I").returnGroup("group").build());
		});
	}

	@Test
	public void testIntToLongCoercion()
	{
		TestUtils.runTest("pkg/TestIntToLongCoercion", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.INT)
					.constant(new GroupConstant(new Literal.Long(3), new FieldExpression("pkg.Constants", "INT_CONST_3", null, true)))
					.build());
		});
	}

	@Test
	public void testIntToLongImplicitCast()
	{
		TestUtils.runTest("pkg/TestIntToLongCoercion", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.LONG)
					.constant(new GroupConstant(new Literal.Long(3), new FieldExpression("pkg.Constants", "INT_CONST_3", DataType.INT, true)))
					.build());
		});
	}

	@Test
	public void testIntToLongFlagCoercion()
	{
		TestUtils.runTest("pkg/TestIntToLongFlagCoercion", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "group")
					.type(GroupType.FLAG)
					.constant(new GroupConstant(new Literal.Long(1), new FieldExpression("pkg.Constants", "INT_FLAG_BIT_0", null, true)))
					.constant(new GroupConstant(new Literal.Long(2), new FieldExpression("pkg.Constants", "INT_FLAG_BIT_1", null, true)))
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.TestIntToLongFlagCoercion", "test1", "()J").returnGroup("group").build());
		});
	}

	@Test
	public void testIntToLongFlagImplicitCast()
	{
		TestUtils.runTest("pkg/TestIntToLongFlagCoercion", data ->
		{
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.LONG, "group")
					.type(GroupType.FLAG)
					.constant(new GroupConstant(new Literal.Long(1), new FieldExpression("pkg.Constants", "INT_FLAG_BIT_0", DataType.INT, true)))
					.constant(new GroupConstant(new Literal.Long(2), new FieldExpression("pkg.Constants", "INT_FLAG_BIT_1", DataType.INT, true)))
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.TestIntToLongFlagCoercion", "test1", "()J").returnGroup("group").build());
		});
	}
}

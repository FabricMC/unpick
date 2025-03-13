package daomephsta.unpick.tests;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.BinaryExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.CastExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.LiteralExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.UnaryExpression;
import daomephsta.unpick.tests.lib.TestUtils;

import org.junit.jupiter.api.Test;

public class TestExpressions
{
	@Test
	public void testExpressions()
	{
		TestUtils.runTest("pkg/TestExpressions", data ->
		{
			Expression const5 = new FieldExpression("pkg.Constants", "INT_CONST_5", null, true);
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.INT)
					.constant(new GroupConstant(new Literal.Long(6), new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.ADD)))
					.constant(new GroupConstant(new Literal.Long(-2), new BinaryExpression(new FieldExpression("pkg.Constants", "INT_CONST_M1", null, true), new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.SUBTRACT)))
					.constant(new GroupConstant(new Literal.Long(10), new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MULTIPLY)))
					.constant(new GroupConstant(new Literal.Long(2), new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.DIVIDE)))
					.constant(new GroupConstant(new Literal.Long(1), new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MODULO)))
					.constant(new GroupConstant(new Literal.Long(20), new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.BIT_SHIFT_LEFT)))
					.constant(new GroupConstant(new Literal.Long(0), new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(3)), BinaryExpression.Operator.BIT_SHIFT_RIGHT)))
					.constant(new GroupConstant(new Literal.Long(128), new BinaryExpression(new FieldExpression("pkg.Constants", "INT_CONST", null, true), new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED)))
					.constant(new GroupConstant(new Literal.Long(-5), new UnaryExpression(const5, UnaryExpression.Operator.NEGATE)))
					.constant(new GroupConstant(new Literal.Long(-6), new UnaryExpression(const5, UnaryExpression.Operator.BIT_NOT)))
					.constant(new GroupConstant(new Literal.Long(1234567890), new CastExpression(DataType.INT, new FieldExpression("pkg.Constants", "LONG_CONST", DataType.LONG, true))))
					.build());

			Expression const1 =  new FieldExpression("pkg.Constants", "LONG_CONST_1", null, true);
			Expression longConst =  new FieldExpression("pkg.Constants", "LONG_CONST", null, true);
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.LONG)
					.constant(new GroupConstant(new Literal.Long(6), new BinaryExpression(const1, new LiteralExpression(new Literal.Integer(4)), BinaryExpression.Operator.ADD)))
					.constant(new GroupConstant(new Literal.Long(-3), new BinaryExpression(new FieldExpression("pkg.Constants", "LONG_CONST_0", null, true), new LiteralExpression(new Literal.Integer(3)), BinaryExpression.Operator.SUBTRACT)))
					.constant(new GroupConstant(new Literal.Long(10), new BinaryExpression(const1, new LiteralExpression(new Literal.Integer(10)), BinaryExpression.Operator.MULTIPLY)))
					.constant(new GroupConstant(new Literal.Long(123456789), new BinaryExpression(longConst, new LiteralExpression(new Literal.Integer(10)), BinaryExpression.Operator.DIVIDE)))
					.constant(new GroupConstant(new Literal.Long(1), new BinaryExpression(longConst, new LiteralExpression(new Literal.Integer(127)), BinaryExpression.Operator.MODULO)))
					.constant(new GroupConstant(new Literal.Long(8), new BinaryExpression(const1, new LiteralExpression(new Literal.Integer(3)), BinaryExpression.Operator.BIT_SHIFT_LEFT)))
					.constant(new GroupConstant(new Literal.Long(0), new BinaryExpression(const1, new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.BIT_SHIFT_RIGHT)))
					.constant(new GroupConstant(new Literal.Long(617283945), new BinaryExpression(longConst, new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED)))
					.constant(new GroupConstant(new Literal.Long(-1), new UnaryExpression(const1, UnaryExpression.Operator.NEGATE)))
					.constant(new GroupConstant(new Literal.Long(-2), new UnaryExpression(const1, UnaryExpression.Operator.BIT_NOT)))
					.build());

			Expression floatConst1 = new FieldExpression("pkg.Constants", "FLOAT_CONST_1", null, true);
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.FLOAT)
					.constant(new GroupConstant(new Literal.Double(3), new BinaryExpression(floatConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.ADD)))
					.constant(new GroupConstant(new Literal.Double(-2), new BinaryExpression(new FieldExpression("pkg.Constants", "FLOAT_CONST_0", null, true), new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.SUBTRACT)))
					.constant(new GroupConstant(new Literal.Double(2), new BinaryExpression(floatConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MULTIPLY)))
					.constant(new GroupConstant(new Literal.Double(0.5), new BinaryExpression(floatConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.DIVIDE)))
					.constant(new GroupConstant(new Literal.Double(1), new BinaryExpression(floatConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MODULO)))
					.constant(new GroupConstant(new Literal.Double(-1), new UnaryExpression(floatConst1, UnaryExpression.Operator.NEGATE)))
					.build());

			Expression doubleConst1 = new FieldExpression("pkg.Constants", "DOUBLE_CONST_1", null, true);
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.DOUBLE)
					.constant(new GroupConstant(new Literal.Double(3), new BinaryExpression(doubleConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.ADD)))
					.constant(new GroupConstant(new Literal.Double(-2), new BinaryExpression(new FieldExpression("pkg.Constants", "DOUBLE_CONST_0", null, true), new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.SUBTRACT)))
					.constant(new GroupConstant(new Literal.Double(2), new BinaryExpression(doubleConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MULTIPLY)))
					.constant(new GroupConstant(new Literal.Double(0.5), new BinaryExpression(doubleConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.DIVIDE)))
					.constant(new GroupConstant(new Literal.Double(1), new BinaryExpression(doubleConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MODULO)))
					.constant(new GroupConstant(new Literal.Double(-1), new UnaryExpression(doubleConst1, UnaryExpression.Operator.NEGATE)))
					.build());
		});
	}
}

package daomephsta.unpick.tests;

import org.junit.jupiter.api.Test;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.BinaryExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.CastExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.LiteralExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.UnaryExpression;
import daomephsta.unpick.tests.lib.TestUtils;

public class TestExpressions {
	@Test
	public void testExpressions() {
		TestUtils.runTest("pkg/TestExpressions", data -> {
			Expression const5 = new FieldExpression("pkg.Constants", "INT_CONST_5", null, true);
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.INT)
					.constant(new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.ADD))
					.constant(new BinaryExpression(new FieldExpression("pkg.Constants", "INT_CONST_M1", null, true), new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.SUBTRACT))
					.constant(new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MULTIPLY))
					.constant(new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.DIVIDE))
					.constant(new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MODULO))
					.constant(new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.BIT_SHIFT_LEFT))
					.constant(new BinaryExpression(const5, new LiteralExpression(new Literal.Integer(3)), BinaryExpression.Operator.BIT_SHIFT_RIGHT))
					.constant(new BinaryExpression(new FieldExpression("pkg.Constants", "INT_CONST", null, true), new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED))
					.constant(new UnaryExpression(const5, UnaryExpression.Operator.NEGATE))
					.constant(new UnaryExpression(const5, UnaryExpression.Operator.BIT_NOT))
					.constant(new CastExpression(DataType.INT, new FieldExpression("pkg.Constants", "LONG_CONST", DataType.LONG, true)))
					.build());

			Expression const1 = new FieldExpression("pkg.Constants", "LONG_CONST_1", null, true);
			Expression longConst = new FieldExpression("pkg.Constants", "LONG_CONST", null, true);
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.LONG)
					.constant(new BinaryExpression(const1, new LiteralExpression(new Literal.Integer(4)), BinaryExpression.Operator.ADD))
					.constant(new BinaryExpression(new FieldExpression("pkg.Constants", "LONG_CONST_0", null, true), new LiteralExpression(new Literal.Integer(3)), BinaryExpression.Operator.SUBTRACT))
					.constant(new BinaryExpression(const1, new LiteralExpression(new Literal.Integer(10)), BinaryExpression.Operator.MULTIPLY))
					.constant(new BinaryExpression(longConst, new LiteralExpression(new Literal.Integer(10)), BinaryExpression.Operator.DIVIDE))
					.constant(new BinaryExpression(longConst, new LiteralExpression(new Literal.Integer(127)), BinaryExpression.Operator.MODULO))
					.constant(new BinaryExpression(const1, new LiteralExpression(new Literal.Integer(3)), BinaryExpression.Operator.BIT_SHIFT_LEFT))
					.constant(new BinaryExpression(const1, new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.BIT_SHIFT_RIGHT))
					.constant(new BinaryExpression(longConst, new LiteralExpression(new Literal.Integer(1)), BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED))
					.constant(new UnaryExpression(const1, UnaryExpression.Operator.NEGATE))
					.constant(new UnaryExpression(const1, UnaryExpression.Operator.BIT_NOT))
					.build());

			Expression floatConst1 = new FieldExpression("pkg.Constants", "FLOAT_CONST_1", null, true);
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.FLOAT)
					.constant(new BinaryExpression(floatConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.ADD))
					.constant(new BinaryExpression(new FieldExpression("pkg.Constants", "FLOAT_CONST_0", null, true), new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.SUBTRACT))
					.constant(new BinaryExpression(floatConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MULTIPLY))
					.constant(new BinaryExpression(floatConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.DIVIDE))
					.constant(new BinaryExpression(floatConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MODULO))
					.constant(new UnaryExpression(floatConst1, UnaryExpression.Operator.NEGATE))
					.build());

			Expression doubleConst1 = new FieldExpression("pkg.Constants", "DOUBLE_CONST_1", null, true);
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.DOUBLE)
					.constant(new BinaryExpression(doubleConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.ADD))
					.constant(new BinaryExpression(new FieldExpression("pkg.Constants", "DOUBLE_CONST_0", null, true), new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.SUBTRACT))
					.constant(new BinaryExpression(doubleConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MULTIPLY))
					.constant(new BinaryExpression(doubleConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.DIVIDE))
					.constant(new BinaryExpression(doubleConst1, new LiteralExpression(new Literal.Integer(2)), BinaryExpression.Operator.MODULO))
					.constant(new UnaryExpression(doubleConst1, UnaryExpression.Operator.NEGATE))
					.build());
		});
	}
}

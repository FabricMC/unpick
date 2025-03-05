package daomephsta.unpick.constantmappers.datadriven.tree.expr;

public final class BinaryExpression extends Expression
{
	public final Expression lhs;
	public final Expression rhs;
	public final Operator operator;

	public BinaryExpression(Expression lhs, Expression rhs, Operator operator)
	{
		this.lhs = lhs;
		this.rhs = rhs;
		this.operator = operator;
	}

	@Override
	public void accept(ExpressionVisitor visitor)
	{
		visitor.visitBinaryExpression(this);
	}

	@Override
	public Expression transform(ExpressionTransformer transformer)
	{
		return transformer.transformBinaryExpression(this);
	}

	public enum Operator
	{
		BIT_OR,
		BIT_XOR,
		BIT_AND,
		BIT_SHIFT_LEFT,
		BIT_SHIFT_RIGHT,
		BIT_SHIFT_RIGHT_UNSIGNED,
		ADD,
		SUBTRACT,
		MULTIPLY,
		DIVIDE,
		MODULO,
	}
}

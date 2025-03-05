package daomephsta.unpick.constantmappers.datadriven.tree.expr;

public abstract class ExpressionTransformer
{
	public Expression transformBinaryExpression(BinaryExpression binaryExpression)
	{
		return new BinaryExpression(binaryExpression.lhs.transform(this), binaryExpression.rhs.transform(this), binaryExpression.operator);
	}

	public Expression transformCastExpression(CastExpression castExpression)
	{
		return new CastExpression(castExpression.castType, castExpression.operand.transform(this));
	}

	public Expression transformFieldExpression(FieldExpression fieldExpression)
	{
		return fieldExpression;
	}

	public Expression transformLiteralExpression(LiteralExpression literalExpression)
	{
		return literalExpression;
	}

	public Expression transformParenExpression(ParenExpression parenExpression)
	{
		return new ParenExpression(parenExpression.expression.transform(this));
	}

	public Expression transformUnaryExpression(UnaryExpression unaryExpression)
	{
		return new UnaryExpression(unaryExpression.operand.transform(this), unaryExpression.operator);
	}
}

package daomephsta.unpick.constantmappers.datadriven.tree.expr;

public final class ParenExpression extends Expression {
	public final Expression expression;

	public ParenExpression(Expression expression) {
		this.expression = expression;
	}

	@Override
	public void accept(ExpressionVisitor visitor) {
		visitor.visitParenExpression(this);
	}

	@Override
	public Expression transform(ExpressionTransformer transformer) {
		return transformer.transformParenExpression(this);
	}
}

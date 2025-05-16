package daomephsta.unpick.constantmappers.datadriven.tree.expr;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

public final class CastExpression extends Expression {
	public final DataType castType;
	public final Expression operand;

	public CastExpression(DataType castType, Expression operand) {
		this.castType = castType;
		this.operand = operand;
	}

	@Override
	public void accept(ExpressionVisitor visitor) {
		visitor.visitCastExpression(this);
	}

	@Override
	public Expression transform(ExpressionTransformer transformer) {
		return transformer.transformCastExpression(this);
	}
}

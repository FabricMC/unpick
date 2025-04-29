package daomephsta.unpick.constantmappers.datadriven.tree.expr;

/**
 * Visitor for an {@link Expression}. By default, recursively visits sub-expressions.
 */
public abstract class ExpressionVisitor {
	public void visitBinaryExpression(BinaryExpression binaryExpression) {
		binaryExpression.lhs.accept(this);
		binaryExpression.rhs.accept(this);
	}

	public void visitCastExpression(CastExpression castExpression) {
		castExpression.operand.accept(this);
	}

	public void visitFieldExpression(FieldExpression fieldExpression) {
	}

	public void visitLiteralExpression(LiteralExpression literalExpression) {
	}

	public void visitParenExpression(ParenExpression parenExpression) {
		parenExpression.expression.accept(this);
	}

	public void visitUnaryExpression(UnaryExpression unaryExpression) {
		unaryExpression.operand.accept(this);
	}
}

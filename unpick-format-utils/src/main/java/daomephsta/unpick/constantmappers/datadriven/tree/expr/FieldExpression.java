package daomephsta.unpick.constantmappers.datadriven.tree.expr;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

public final class FieldExpression extends Expression {
	public final String className;
	@Nullable
	public final String fieldName;
	@Nullable
	public final DataType fieldType;
	public final boolean isStatic;

	public FieldExpression(String className, @Nullable String fieldName, @Nullable DataType fieldType, boolean isStatic) {
		this.className = className;
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.isStatic = isStatic;
	}

	@Override
	public void accept(ExpressionVisitor visitor) {
		visitor.visitFieldExpression(this);
	}

	@Override
	public Expression transform(ExpressionTransformer transformer) {
		return transformer.transformFieldExpression(this);
	}
}

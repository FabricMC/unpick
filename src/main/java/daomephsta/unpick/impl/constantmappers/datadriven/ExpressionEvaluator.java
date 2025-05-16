package daomephsta.unpick.impl.constantmappers.datadriven;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.BinaryExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.CastExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionVisitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.LiteralExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.UnaryExpression;
import daomephsta.unpick.impl.DataTypeUtils;

public final class ExpressionEvaluator extends ExpressionVisitor {
	private final IConstantResolver constantResolver;
	private final IInheritanceChecker inheritanceChecker;
	@Nullable
	private Object result;

	public ExpressionEvaluator(IConstantResolver constantResolver, IInheritanceChecker inheritanceChecker) {
		this.constantResolver = constantResolver;
		this.inheritanceChecker = inheritanceChecker;
	}

	@Nullable
	public Object getResult() {
		return result;
	}

	@Override
	public void visitBinaryExpression(BinaryExpression binaryExpression) {
		binaryExpression.lhs.accept(this);
		Object left = result;
		binaryExpression.rhs.accept(this);
		Object right = result;

		DataType leftType = DataTypeUtils.getDataType(left);
		DataType rightType = DataTypeUtils.getDataType(right);
		DataType resultType = getBinaryResultType(leftType, rightType);

		result = switch (binaryExpression.operator) {
			case BIT_OR -> DataTypeUtils.cast(upcastAsLong(left) | upcastAsLong(right), resultType);
			case BIT_XOR -> DataTypeUtils.cast(upcastAsLong(left) ^ upcastAsLong(right), resultType);
			case BIT_AND -> DataTypeUtils.cast(upcastAsLong(left) & upcastAsLong(right), resultType);
			case BIT_SHIFT_LEFT -> DataTypeUtils.cast(upcastAsLong(left) << upcastAsLong(right), resultType);
			case BIT_SHIFT_RIGHT -> DataTypeUtils.cast(upcastAsLong(left) >> upcastAsLong(right), resultType);
			case BIT_SHIFT_RIGHT_UNSIGNED -> DataTypeUtils.cast(upcastAsLongUnsigned(left) >>> upcastAsLong(right), resultType);
			case ADD -> {
				if (left instanceof String) {
					yield left + asString(right);
				} else if (right instanceof String) {
					yield asString(left) + right;
				} else {
					Long leftInteger = upcastAsLongOrNull(left);
					Long rightInteger = upcastAsLongOrNull(right);
					if (leftInteger != null && rightInteger != null) {
						yield DataTypeUtils.cast(leftInteger + rightInteger, resultType);
					} else {
						yield DataTypeUtils.cast(upcastAsDouble(left) + upcastAsDouble(right), resultType);
					}
				}
			}
			case SUBTRACT -> {
				Long leftInteger = upcastAsLongOrNull(left);
				Long rightInteger = upcastAsLongOrNull(right);
				if (leftInteger != null && rightInteger != null) {
					yield DataTypeUtils.cast(leftInteger - rightInteger, resultType);
				} else {
					yield DataTypeUtils.cast(upcastAsDouble(left) - upcastAsDouble(right), resultType);
				}
			}
			case MULTIPLY -> {
				Long leftInteger = upcastAsLongOrNull(left);
				Long rightInteger = upcastAsLongOrNull(right);
				if (leftInteger != null && rightInteger != null) {
					yield DataTypeUtils.cast(leftInteger * rightInteger, resultType);
				} else {
					yield DataTypeUtils.cast(upcastAsDouble(left) * upcastAsDouble(right), resultType);
				}
			}
			case DIVIDE -> {
				Long leftInteger = upcastAsLongOrNull(left);
				Long rightInteger = upcastAsLongOrNull(right);
				if (leftInteger != null && rightInteger != null) {
					if (rightInteger == 0) {
						throw new UnpickSyntaxException("Cannot divide " + leftInteger + " by zero");
					}
					yield DataTypeUtils.cast(leftInteger / rightInteger, resultType);
				} else {
					yield DataTypeUtils.cast(upcastAsDouble(left) / upcastAsDouble(right), resultType);
				}
			}
			case MODULO -> {
				Long leftInteger = upcastAsLongOrNull(left);
				Long rightInteger = upcastAsLongOrNull(right);
				if (leftInteger != null && rightInteger != null) {
					if (rightInteger == 0) {
						throw new UnpickSyntaxException("Cannot divide " + leftInteger + " by zero");
					}
					yield DataTypeUtils.cast(leftInteger % rightInteger, resultType);
				} else {
					yield DataTypeUtils.cast(upcastAsDouble(left) % upcastAsDouble(right), resultType);
				}
			}
		};
	}

	@Override
	public void visitCastExpression(CastExpression castExpression) {
		castExpression.operand.accept(this);
		result = DataTypeUtils.cast(result, castExpression.castType);
	}

	@Override
	public void visitFieldExpression(FieldExpression fieldExpression) {
		String ownerName = fieldExpression.className.replace('.', '/');
		String fieldName = Objects.requireNonNull(fieldExpression.fieldName, "Cannot resolve wildcard field expression");
		IConstantResolver.ResolvedConstant resolvedConstant = constantResolver.resolveConstant(ownerName, fieldName);
		if (resolvedConstant == null) {
			throw new UnpickSyntaxException("Could not resolve constant field " + fieldExpression.className + "." + fieldExpression.fieldName);
		}
		if (fieldExpression.fieldType != null && fieldExpression.fieldType != DataTypeUtils.asmTypeToDataType(resolvedConstant.type())) {
			throw new UnpickSyntaxException("Resolved field " + fieldExpression.className + "." + fieldExpression.fieldName + " has different type than expected");
		}
		if (fieldExpression.isStatic != resolvedConstant.isStatic()) {
			throw new UnpickSyntaxException("Staticness of resolved field " + fieldExpression.className + "." + fieldExpression.fieldName + " does not match expected staticness");
		}
		result = resolvedConstant.value();
	}

	@Override
	public void visitLiteralExpression(LiteralExpression literalExpression) {
		Literal literal = literalExpression.literal;
		result = switch (literal) {
			case Literal.Integer(int value, int ignored) -> value;
			case Literal.Long(long value, int ignored) -> value;
			case Literal.Float(float value) -> value;
			case Literal.Double(double value) -> value;
			case Literal.Character(char value) -> value;
			case Literal.String(String value) -> value;
		};
	}

	@Override
	public void visitUnaryExpression(UnaryExpression unaryExpression) {
		unaryExpression.operand.accept(this);
		DataType resultType = DataTypeUtils.widenNarrowTypes(DataTypeUtils.getDataType(result));
		result = switch (unaryExpression.operator) {
			case NEGATE -> {
				Long integer = upcastAsLongOrNull(result);
				if (integer != null) {
					yield DataTypeUtils.cast(-integer, resultType);
				} else {
					yield DataTypeUtils.cast(-upcastAsDouble(result), resultType);
				}
			}
			case BIT_NOT -> DataTypeUtils.cast(~upcastAsLong(result), resultType);
		};
	}

	private long upcastAsLong(Object value) {
		Long result = upcastAsLongOrNull(value);
		if (result == null) {
			throw new UnpickSyntaxException("Cannot interpret " + asString(value) + " as an integer");
		}
		return result;
	}

	@Nullable
	private static Long upcastAsLongOrNull(Object value) {
		if (!DataTypeUtils.isAssignable(DataType.LONG, DataTypeUtils.getDataType(value))) {
			return null;
		}
		return (Long) DataTypeUtils.cast(value, DataType.LONG);
	}

	private long upcastAsLongUnsigned(Object value) {
		long result = upcastAsLong(value);
		return value instanceof Long ? result : result & 0xffffffffL;
	}

	private double upcastAsDouble(Object value) {
		Double result = upcastAsDoubleOrNull(value);
		if (result == null) {
			throw new UnpickSyntaxException("Cannot interpret " + asString(value) + " as a number");
		}
		return result;
	}

	@Nullable
	private static Double upcastAsDoubleOrNull(Object value) {
		if (!DataTypeUtils.isAssignable(DataType.DOUBLE, DataTypeUtils.getDataType(value))) {
			return null;
		}
		return (Double) DataTypeUtils.cast(value, DataType.DOUBLE);
	}

	public static DataType getBinaryResultType(DataType type1, DataType type2) {
		if (type1 == DataType.STRING || type2 == DataType.STRING) {
			return DataType.STRING;
		} else if (DataTypeUtils.isPrimitive(type1) && DataTypeUtils.isPrimitive(type2)) {
			return DataTypeUtils.widenNarrowTypes(DataTypeUtils.getCommonSuperType(type1, type2));
		} else {
			// return something arbitrary, the appropriate error will be thrown when trying to convert an operand to a number
			return type1;
		}
	}

	private String asString(Object value) {
		if (value instanceof Type type) {
			return switch (type.getSort()) {
				case Type.ARRAY -> "class " + type.getDescriptor().replace('/', '.');
				case Type.OBJECT -> {
					IInheritanceChecker.ClassInfo classInfo = inheritanceChecker.getClassInfo(type.getInternalName());
					if (classInfo != null && classInfo.isInterface()) {
						yield "interface " + type.getInternalName().replace('/', '.');
					} else {
						yield "class " + type.getInternalName().replace('/', '.');
					}
				}
				default -> type.getClassName();
			};
		} else {
			return String.valueOf(value);
		}
	}
}

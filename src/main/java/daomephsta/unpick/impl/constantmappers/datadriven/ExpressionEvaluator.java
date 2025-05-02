package daomephsta.unpick.impl.constantmappers.datadriven;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.BinaryExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.CastExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionVisitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.LiteralExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.UnaryExpression;
import daomephsta.unpick.impl.Utils;

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

		result = switch (binaryExpression.operator) {
			case BIT_OR -> downcastPrimitive(upcastAsLong(left) | upcastAsLong(right), left, right);
			case BIT_XOR -> downcastPrimitive(upcastAsLong(left) ^ upcastAsLong(right), left, right);
			case BIT_AND -> downcastPrimitive(upcastAsLong(left) & upcastAsLong(right), left, right);
			case BIT_SHIFT_LEFT -> downcastPrimitive(upcastAsLong(left) << upcastAsLong(right), left, right);
			case BIT_SHIFT_RIGHT -> downcastPrimitive(upcastAsLong(left) >> upcastAsLong(right), left, right);
			case BIT_SHIFT_RIGHT_UNSIGNED -> downcastPrimitive(upcastAsLongUnsigned(left) >>> upcastAsLong(right), left, right);
			case ADD -> {
				if (left instanceof String) {
					yield left + asString(right);
				} else if (right instanceof String) {
					yield asString(left) + right;
				} else {
					Long leftInteger = upcastAsLongOrNull(left);
					Long rightInteger = upcastAsLongOrNull(right);
					if (leftInteger != null && rightInteger != null) {
						yield downcastPrimitive(leftInteger + rightInteger, left, right);
					} else {
						yield downcastPrimitive(upcastAsDouble(left) + upcastAsDouble(right), left, right);
					}
				}
			}
			case SUBTRACT -> {
				Long leftInteger = upcastAsLongOrNull(left);
				Long rightInteger = upcastAsLongOrNull(right);
				if (leftInteger != null && rightInteger != null) {
					yield downcastPrimitive(leftInteger - rightInteger, left, right);
				} else {
					yield downcastPrimitive(upcastAsDouble(left) - upcastAsDouble(right), left, right);
				}
			}
			case MULTIPLY -> {
				Long leftInteger = upcastAsLongOrNull(left);
				Long rightInteger = upcastAsLongOrNull(right);
				if (leftInteger != null && rightInteger != null) {
					yield downcastPrimitive(leftInteger * rightInteger, left, right);
				} else {
					yield downcastPrimitive(upcastAsDouble(left) * upcastAsDouble(right), left, right);
				}
			}
			case DIVIDE -> {
				Long leftInteger = upcastAsLongOrNull(left);
				Long rightInteger = upcastAsLongOrNull(right);
				if (leftInteger != null && rightInteger != null) {
					if (rightInteger == 0) {
						throw new UnpickSyntaxException("Cannot divide " + leftInteger + " by zero");
					}
					yield downcastPrimitive(leftInteger / rightInteger, left, right);
				} else {
					yield downcastPrimitive(upcastAsDouble(left) / upcastAsDouble(right), left, right);
				}
			}
			case MODULO -> {
				Long leftInteger = upcastAsLongOrNull(left);
				Long rightInteger = upcastAsLongOrNull(right);
				if (leftInteger != null && rightInteger != null) {
					if (rightInteger == 0) {
						throw new UnpickSyntaxException("Cannot divide " + leftInteger + " by zero");
					}
					yield downcastPrimitive(leftInteger % rightInteger, left, right);
				} else {
					yield downcastPrimitive(upcastAsDouble(left) % upcastAsDouble(right), left, right);
				}
			}
		};
	}

	@Override
	public void visitCastExpression(CastExpression castExpression) {
		castExpression.operand.accept(this);
		Number operand;
		if (result instanceof Number number) {
			operand = number;
		} else if (result instanceof Character character) {
			operand = (int) character;
		} else {
			throw new UnpickSyntaxException("Cannot interpret " + asString(result) + " as a number");
		}
		result = switch (castExpression.castType) {
			case BYTE -> operand.byteValue();
			case SHORT -> operand.shortValue();
			case CHAR -> (char) operand.intValue();
			case INT -> operand.intValue();
			case LONG -> operand.longValue();
			case FLOAT -> operand.floatValue();
			case DOUBLE -> operand.doubleValue();
			default -> throw new UnpickSyntaxException("Cannot cast " + asString(result) + " to " + castExpression.castType);
		};
	}

	@Override
	public void visitFieldExpression(FieldExpression fieldExpression) {
		String ownerName = fieldExpression.className.replace('.', '/');
		String fieldName = Objects.requireNonNull(fieldExpression.fieldName, "Cannot resolve wildcard field expression");
		IConstantResolver.ResolvedConstant resolvedConstant = constantResolver.resolveConstant(ownerName, fieldName);
		if (resolvedConstant == null) {
			throw new UnpickSyntaxException("Could not resolve constant field " + fieldExpression.className + "." + fieldExpression.fieldName);
		}
		if (fieldExpression.fieldType != null && fieldExpression.fieldType != Utils.asmTypeToDataType(resolvedConstant.type())) {
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
		if (literal instanceof Literal.Integer integer) {
			result = integer.value();
		} else if (literal instanceof Literal.Long longLiteral) {
			result = longLiteral.value();
		} else if (literal instanceof Literal.Float floatLiteral) {
			result = floatLiteral.value();
		} else if (literal instanceof Literal.Double doubleLiteral) {
			result = doubleLiteral.value();
		} else if (literal instanceof Literal.Character character) {
			result = character.value();
		} else if (literal instanceof Literal.String string) {
			result = string.value();
		} else {
			throw new AssertionError("Unknown literal type " + literal.getClass().getName());
		}
	}

	@Override
	public void visitUnaryExpression(UnaryExpression unaryExpression) {
		unaryExpression.operand.accept(this);
		result = switch (unaryExpression.operator) {
			case NEGATE -> {
				Long integer = upcastAsLongOrNull(result);
				if (integer != null) {
					yield downcastPrimitive(-integer, result);
				} else {
					yield downcastPrimitive(-upcastAsDouble(result), result);
				}
			}
			case BIT_NOT -> downcastPrimitive(~upcastAsLong(result), result);
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
		if (value instanceof Character character) {
			return (long) character;
		}
		if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
			return ((Number) value).longValue();
		} else if (value instanceof Long longValue) {
			return longValue;
		} else {
			return null;
		}
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
		if (value instanceof Double doubleValue) {
			return doubleValue;
		} else if (value instanceof Character character) {
			return (double) character;
		} else if (value instanceof Number number) {
			return number.doubleValue();
		} else {
			return null;
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

	private static Object downcastPrimitive(Object value, Object expectedType) {
		Long integer = upcastAsLongOrNull(value);
		if (integer != null) {
			if (expectedType instanceof Long) {
				return integer;
			} else {
				return integer.intValue();
			}
		}
		Double doubleValue = Objects.requireNonNull(upcastAsDoubleOrNull(value), "Input is not a primitive");
		if (expectedType instanceof Long) {
			return doubleValue.longValue();
		} else if (expectedType instanceof Float) {
			return doubleValue.floatValue();
		} else if (expectedType instanceof Double) {
			return doubleValue;
		} else {
			return doubleValue.intValue();
		}
	}

	private static Object downcastPrimitive(Object value, Object expectedType1, Object expectedType2) {
		if (expectedType1 instanceof Double) {
			return downcastPrimitive(value, expectedType1);
		}
		if (expectedType2 instanceof Double) {
			return downcastPrimitive(value, expectedType2);
		}
		if (expectedType1 instanceof Float) {
			return downcastPrimitive(value, expectedType1);
		}
		if (expectedType2 instanceof Float) {
			return downcastPrimitive(value, expectedType2);
		}
		if (expectedType1 instanceof Long) {
			return downcastPrimitive(value, expectedType1);
		}
		if (expectedType2 instanceof Long) {
			return downcastPrimitive(value, expectedType2);
		}
		return downcastPrimitive(value, expectedType1);
	}
}

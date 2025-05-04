package daomephsta.unpick.impl;

import java.util.Locale;
import java.util.function.Function;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

public final class DataTypeUtils {
	private DataTypeUtils() {
	}

	public static DataType asmTypeToDataType(Type type) {
		return switch (type.getSort()) {
			case Type.BYTE -> DataType.BYTE;
			case Type.SHORT -> DataType.SHORT;
			case Type.INT -> DataType.INT;
			case Type.LONG -> DataType.LONG;
			case Type.FLOAT -> DataType.FLOAT;
			case Type.DOUBLE -> DataType.DOUBLE;
			case Type.CHAR -> DataType.CHAR;
			case Type.OBJECT -> switch (type.getInternalName()) {
				case "java/lang/String" -> DataType.STRING;
				case "java/lang/Class" -> DataType.CLASS;
				default -> null;
			};
			default -> null;
		};
	}

	public static String getDescriptor(DataType dataType) {
		return switch (dataType) {
			case BYTE -> "B";
			case SHORT -> "S";
			case INT -> "I";
			case LONG -> "J";
			case FLOAT -> "F";
			case DOUBLE -> "D";
			case CHAR -> "C";
			case STRING -> "Ljava/lang/String;";
			case CLASS -> "Ljava/lang/Class;";
		};
	}

	@Nullable
	@Contract("null -> null; !null -> !null")
	public static DataType getDataType(@Nullable Object value) {
		return switch (value) {
			case Byte ignored -> DataType.BYTE;
			case Short ignored -> DataType.SHORT;
			case Integer ignored -> DataType.INT;
			case Long ignored -> DataType.LONG;
			case Float ignored -> DataType.FLOAT;
			case Double ignored -> DataType.DOUBLE;
			case Character ignored -> DataType.CHAR;
			case String ignored -> DataType.STRING;
			case Type ignored -> DataType.CLASS;
			case null -> null;
			default -> throw new AssertionError("Invalid constant data type: " + value.getClass().getName());
		};
	}

	@Nullable
	@Contract("null, _ -> null; !null, _ -> !null")
	public static Object cast(@Nullable Object value, @Nullable DataType dataType) {
		if (value == null) {
			if (isPrimitive(dataType)) {
				throw new UnpickSyntaxException("Cannot cast null to " + getTypeName(dataType));
			} else {
				return null;
			}
		}

		Object result = tryCast(value, dataType);
		if (result == null) {
			throw new UnpickSyntaxException("Cannot cast " + value + " to " + getTypeName(dataType));
		}
		return result;
	}

	@Nullable
	public static Object tryCast(@Nullable Object value, @Nullable DataType dataType) {
		return switch (dataType) {
			case BYTE -> tryCastToNumber(value, Number::byteValue);
			case SHORT -> tryCastToNumber(value, Number::shortValue);
			case INT -> tryCastToNumber(value, Number::intValue);
			case LONG -> tryCastToNumber(value, Number::longValue);
			case FLOAT -> tryCastToNumber(value, Number::floatValue);
			case DOUBLE -> tryCastToNumber(value, Number::doubleValue);
			case CHAR -> tryCastToNumber(value, n -> (char) n.intValue());
			case STRING -> value instanceof String ? value : null;
			case CLASS -> value instanceof Type ? value : null;
			case null -> value instanceof String || value instanceof Type ? value : null;
		};
	}

	@Nullable
	private static Object tryCastToNumber(@Nullable Object value, Function<Number, Object> castFunction) {
		return switch (value) {
			case Character c -> castFunction.apply((int) c);
			case Number n -> castFunction.apply(n);
			case null, default -> null;
		};
	}

	@Nullable
	@Contract("null, _ -> null; !null, _ -> !null")
	public static Object castExact(@Nullable Object value, @Nullable DataType dataType) {
		if (value == null) {
			if (isPrimitive(dataType)) {
				throw new UnpickSyntaxException("Cannot cast null exactly to " + getTypeName(dataType));
			} else {
				return null;
			}
		}

		Object result = tryCastExact(value, dataType);
		if (result == null) {
			throw new UnpickSyntaxException("Cannot cast " + value + " exactly to " + getTypeName(dataType));
		}
		return result;
	}

	@Nullable
	public static Object tryCastExact(@Nullable Object value, @Nullable DataType dataType) {
		return switch (dataType) {
			case BYTE -> tryCastToDoubleFittingNumberExact(value, Number::byteValue);
			case SHORT -> tryCastToDoubleFittingNumberExact(value, Number::shortValue);
			case INT -> tryCastToDoubleFittingNumberExact(value, Number::intValue);
			case LONG -> switch (value) {
				case Float f -> (float) f.longValue() == f ? f.longValue() : null;
				case Double d -> (double) d.longValue() == d ? d.longValue() : null;
				case Number n -> n.longValue();
				case null, default -> null;
			};
			case FLOAT -> tryCastToDoubleFittingNumberExact(value, Number::floatValue);
			case DOUBLE -> tryCastToDoubleFittingNumberExact(value, Number::doubleValue);
			case CHAR -> {
				Integer result = tryCastToDoubleFittingNumberExact(value, Number::intValue);
				yield result == null ? null : (char) result.intValue();
			}
			case STRING -> value instanceof String ? value : null;
			case CLASS -> value instanceof Type ? value : null;
			case null -> value instanceof String || value instanceof Type ? value : null;
		};
	}

	private static <T extends Number> T tryCastToDoubleFittingNumberExact(@Nullable Object value, Function<Number, T> castFunction) {
		return switch (value) {
			case Character c -> {
				T result = castFunction.apply((int) c);
				yield result.intValue() == (int) c ? result : null;
			}
			case Number n -> {
				T result = castFunction.apply(n);
				yield result.doubleValue() == n.doubleValue() ? result : null;
			}
			case null, default -> null;
		};
	}

	public static boolean isPrimitive(@Nullable DataType dataType) {
		return dataType != DataType.STRING && dataType != DataType.CLASS && dataType != null;
	}

	public static boolean isAssignable(@Nullable DataType lhs, @Nullable DataType rhs) {
		if (lhs == rhs) {
			return true;
		}
		if (lhs == null) {
			return false;
		}
		if (rhs == null) {
			return !isPrimitive(lhs);
		}
		if (!isPrimitive(lhs) || !isPrimitive(rhs)) {
			return false;
		}
		if (lhs == DataType.CHAR) {
			return false;
		}
		if (rhs == DataType.CHAR) {
			return lhs.ordinal() >= DataType.INT.ordinal();
		}
		return lhs.ordinal() >= rhs.ordinal();
	}

	public static DataType getCommonSuperType(DataType type1, DataType type2) {
		if (isAssignable(type1, type2)) {
			return type1;
		} else if (isAssignable(type2, type1)) {
			return type2;
		} else if (isPrimitive(type1) && isPrimitive(type2)) {
			// possible if either type is a char
			return DataType.INT;
		} else {
			throw new UnpickSyntaxException("Cannot find common super type of " + getTypeName(type1) + " and " + getTypeName(type2));
		}
	}

	@Nullable
	@Contract("null -> null; !null -> !null")
	public static DataType widenNarrowTypes(@Nullable DataType dataType) {
		return switch (dataType) {
			case BYTE, SHORT, CHAR -> DataType.INT;
			case null, default -> dataType;
		};
	}

	private static String getTypeName(@Nullable DataType dataType) {
		return switch (dataType) {
			case STRING -> "String";
			case CLASS -> "Class";
			case null -> "null";
			default -> dataType.name().toLowerCase(Locale.ROOT);
		};
	}
}

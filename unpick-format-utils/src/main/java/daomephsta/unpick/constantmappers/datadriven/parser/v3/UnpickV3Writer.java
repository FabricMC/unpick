package daomephsta.unpick.constantmappers.datadriven.parser.v3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.BinaryExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.CastExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionVisitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.LiteralExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ParenExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.UnaryExpression;

/**
 * A visitor that generates .unpick v3 format text. Useful for programmatically writing .unpick v3 format files;
 * or remapping them, when used as the delegate for an instance of {@link UnpickV3Remapper}.
 */
public final class UnpickV3Writer extends UnpickV3Visitor {
	private static final String LINE_SEPARATOR = System.lineSeparator();
	private final String indent;
	private final StringBuilder output = new StringBuilder("unpick v3").append(LINE_SEPARATOR);

	public UnpickV3Writer() {
		this("\t");
	}

	public UnpickV3Writer(String indent) {
		this.indent = indent;
	}

	@Override
	public void visitGroupDefinition(GroupDefinition groupDefinition) {
		output.append(LINE_SEPARATOR);

		if (!(groupDefinition.scope instanceof GroupScope.Global)) {
			writeGroupScope(groupDefinition.scope);
			output.append(" ");
		}

		writeLowerCaseEnum(groupDefinition.type);
		output.append(" ");

		if (groupDefinition.strict) {
			output.append("strict ");
		}

		writeDataType(groupDefinition.dataType);

		if (groupDefinition.name != null) {
			output.append(" ").append(groupDefinition.name);
		}

		output.append(LINE_SEPARATOR);

		if (groupDefinition.format != null) {
			output.append(indent).append("format = ");
			writeLowerCaseEnum(groupDefinition.format);
			output.append(LINE_SEPARATOR);
		}

		for (GroupConstant constant : groupDefinition.constants) {
			writeGroupConstant(constant);
		}
	}

	private void writeGroupScope(GroupScope scope) {
		output.append("scoped ");
		if (scope instanceof GroupScope.Package) {
			output.append("package ").append(((GroupScope.Package) scope).packageName);
		} else if (scope instanceof GroupScope.Class) {
			output.append("class ").append(((GroupScope.Class) scope).className);
		} else if (scope instanceof GroupScope.Method) {
			GroupScope.Method methodScope = (GroupScope.Method) scope;
			output.append("method ")
					.append(methodScope.className)
					.append(" ")
					.append(methodScope.methodName)
					.append(" ")
					.append(methodScope.methodDesc);
		} else {
			throw new AssertionError("Unknown group scope type: " + scope.getClass().getName());
		}
	}

	private void writeGroupConstant(GroupConstant constant) {
		output.append(indent);
		writeGroupConstantKey(constant.key);
		output.append(" = ");
		constant.value.accept(new ExpressionWriter());
		output.append(LINE_SEPARATOR);
	}

	private void writeGroupConstantKey(Literal.ConstantKey constantKey) {
		if (constantKey instanceof Literal.Long) {
			Literal.Long longLiteral = (Literal.Long) constantKey;
			if (longLiteral.radix == 10) {
				// treat base 10 as signed
				output.append(longLiteral.value);
			} else {
				writeRadixPrefix(longLiteral.radix);
				output.append(Long.toUnsignedString(longLiteral.value, longLiteral.radix));
			}
		} else if (constantKey instanceof Literal.Double) {
			output.append(((Literal.Double) constantKey).value);
		} else if (constantKey instanceof Literal.String) {
			output.append(quoteString(((Literal.String) constantKey).value, '"'));
		} else if (constantKey instanceof Literal.Class) {
			output.append("class ").append(((Literal.Class) constantKey).descriptor);
		} else if (constantKey instanceof Literal.Null) {
			output.append("null");
		} else {
			throw new AssertionError("Unknown group constant key type: " + constantKey.getClass().getName());
		}
	}

	@Override
	public void visitTargetField(TargetField targetField) {
		output.append(LINE_SEPARATOR)
				.append("target_field ")
				.append(targetField.className)
				.append(" ")
				.append(targetField.fieldName)
				.append(" ")
				.append(targetField.fieldDesc)
				.append(" ")
				.append(targetField.groupName)
				.append(LINE_SEPARATOR);
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod) {
		output.append(LINE_SEPARATOR)
				.append("target_method ")
				.append(targetMethod.className)
				.append(" ")
				.append(targetMethod.methodName)
				.append(" ")
				.append(targetMethod.methodDesc)
				.append(LINE_SEPARATOR);

		List<Map.Entry<Integer, String>> paramGroups = new ArrayList<>(targetMethod.paramGroups.entrySet());
		paramGroups.sort(Map.Entry.comparingByKey());
		for (Map.Entry<Integer, String> paramGroup : paramGroups) {
			output.append(indent)
					.append("param ")
					.append(paramGroup.getKey())
					.append(" ")
					.append(paramGroup.getValue())
					.append(LINE_SEPARATOR);
		}

		if (targetMethod.returnGroup != null) {
			output.append(indent)
					.append("return ")
					.append(targetMethod.returnGroup)
					.append(LINE_SEPARATOR);
		}
	}

	private void writeRadixPrefix(int radix) {
		switch (radix) {
			case 10:
				break;
			case 16:
				output.append("0x");
				break;
			case 8:
				output.append("0");
				break;
			case 2:
				output.append("0b");
				break;
			default:
				throw new AssertionError("Illegal radix: " + radix);
		}
	}

	private void writeDataType(DataType dataType) {
		switch (dataType) {
			case STRING:
				output.append("String");
				break;
			case CLASS:
				output.append("Class");
				break;
			default:
				writeLowerCaseEnum(dataType);
				break;
		}
	}

	private void writeLowerCaseEnum(Enum<?> enumValue) {
		output.append(enumValue.name().toLowerCase(Locale.ROOT));
	}

	static String quoteString(String string, char quoteChar) {
		StringBuilder result = new StringBuilder(string.length() + 2).append(quoteChar);

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch (c) {
				case '\b':
					result.append("\\b");
					break;
				case '\t':
					result.append("\\t");
					break;
				case '\n':
					result.append("\\n");
					break;
				case '\f':
					result.append("\\f");
					break;
				case '\r':
					result.append("\\r");
					break;
				case '\\':
					result.append("\\\\");
					break;
				default:
					if (c == quoteChar) {
						result.append("\\").append(c);
					} else if (isPrintable(c)) {
						result.append(c);
					} else if (c <= 255) {
						result.append('\\').append(Integer.toOctalString(c));
					} else {
						result.append("\\u").append(String.format("%04x", (int) c));
					}
			}
		}

		return result.append(quoteChar).toString();
	}

	private static boolean isPrintable(char ch) {
		switch (Character.getType(ch)) {
			case Character.UPPERCASE_LETTER:
			case Character.LOWERCASE_LETTER:
			case Character.TITLECASE_LETTER:
			case Character.MODIFIER_LETTER:
			case Character.OTHER_LETTER:
			case Character.NON_SPACING_MARK:
			case Character.ENCLOSING_MARK:
			case Character.COMBINING_SPACING_MARK:
			case Character.DECIMAL_DIGIT_NUMBER:
			case Character.LETTER_NUMBER:
			case Character.OTHER_NUMBER:
			case Character.SPACE_SEPARATOR:
			case Character.DASH_PUNCTUATION:
			case Character.START_PUNCTUATION:
			case Character.END_PUNCTUATION:
			case Character.CONNECTOR_PUNCTUATION:
			case Character.OTHER_PUNCTUATION:
			case Character.MATH_SYMBOL:
			case Character.CURRENCY_SYMBOL:
			case Character.MODIFIER_SYMBOL:
			case Character.OTHER_SYMBOL:
			case Character.INITIAL_QUOTE_PUNCTUATION:
			case Character.FINAL_QUOTE_PUNCTUATION:
				return true;
		}
		return false;
	}

	public String getOutput() {
		return output.toString();
	}

	private final class ExpressionWriter extends ExpressionVisitor {
		@Override
		public void visitBinaryExpression(BinaryExpression binaryExpression) {
			binaryExpression.lhs.accept(this);
			switch (binaryExpression.operator) {
				case BIT_OR:
					output.append(" | ");
					break;
				case BIT_XOR:
					output.append(" ^ ");
					break;
				case BIT_AND:
					output.append(" & ");
					break;
				case BIT_SHIFT_LEFT:
					output.append(" << ");
					break;
				case BIT_SHIFT_RIGHT:
					output.append(" >> ");
					break;
				case BIT_SHIFT_RIGHT_UNSIGNED:
					output.append(" >>> ");
					break;
				case ADD:
					output.append(" + ");
					break;
				case SUBTRACT:
					output.append(" - ");
					break;
				case MULTIPLY:
					output.append(" * ");
					break;
				case DIVIDE:
					output.append(" / ");
					break;
				case MODULO:
					output.append(" % ");
					break;
				default:
					throw new AssertionError("Unknown operator: " + binaryExpression.operator);
			}
			binaryExpression.rhs.accept(this);
		}

		@Override
		public void visitCastExpression(CastExpression castExpression) {
			output.append('(');
			writeDataType(castExpression.castType);
			output.append(") ");
			castExpression.operand.accept(this);
		}

		@Override
		public void visitFieldExpression(FieldExpression fieldExpression) {
			output.append(fieldExpression.className).append('.').append(fieldExpression.fieldName);
			if (!fieldExpression.isStatic) {
				output.append(":instance");
			}
			if (fieldExpression.fieldType != null) {
				output.append(':');
				writeDataType(fieldExpression.fieldType);
			}
		}

		@Override
		public void visitLiteralExpression(LiteralExpression literalExpression) {
			if (literalExpression.literal instanceof Literal.Integer) {
				Literal.Integer literalInteger = (Literal.Integer) literalExpression.literal;
				writeRadixPrefix(literalInteger.radix);
				output.append(Integer.toUnsignedString(literalInteger.value, literalInteger.radix));
			} else if (literalExpression.literal instanceof Literal.Long) {
				Literal.Long literalLong = (Literal.Long) literalExpression.literal;
				writeRadixPrefix(literalLong.radix);
				output.append(Long.toUnsignedString(literalLong.value, literalLong.radix)).append('L');
			} else if (literalExpression.literal instanceof Literal.Float) {
				output.append(((Literal.Float) literalExpression.literal).value).append('F');
			} else if (literalExpression.literal instanceof Literal.Double) {
				output.append(((Literal.Double) literalExpression.literal).value);
			} else if (literalExpression.literal instanceof Literal.Character) {
				output.append(quoteString(String.valueOf(((Literal.Character) literalExpression.literal).value), '\''));
			} else if (literalExpression.literal instanceof Literal.String) {
				output.append(quoteString(((Literal.String) literalExpression.literal).value, '"'));
			} else {
				throw new AssertionError("Unknown literal: " + literalExpression.literal);
			}
		}

		@Override
		public void visitParenExpression(ParenExpression parenExpression) {
			output.append('(');
			parenExpression.expression.accept(this);
			output.append(')');
		}

		@Override
		public void visitUnaryExpression(UnaryExpression unaryExpression) {
			switch (unaryExpression.operator) {
				case NEGATE:
					output.append('-');
					break;
				case BIT_NOT:
					output.append('~');
					break;
				default:
					throw new AssertionError("Unknown operator: " + unaryExpression.operator);
			}
			unaryExpression.operand.accept(this);
		}
	}
}

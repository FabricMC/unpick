package daomephsta.unpick.constantmappers.datadriven.parser.v3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.BinaryExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.CastExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
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

		if (groupDefinition.docs() != null) {
			for (String docLine : groupDefinition.docs().split("\n", -1)) {
				output.append("#: ").append(docLine).append(LINE_SEPARATOR);
			}
		}

		output.append("group ");
		writeDataType(groupDefinition.dataType());

		if (groupDefinition.name() != null) {
			output.append(" ").append(groupDefinition.name());
		}

		output.append(LINE_SEPARATOR);

		for (GroupScope scope : groupDefinition.scopes()) {
			output.append(indent);
			writeGroupScope(scope);
			output.append(LINE_SEPARATOR);
		}

		if (groupDefinition.flags()) {
			output.append(indent).append("@flags").append(LINE_SEPARATOR);
		}

		if (groupDefinition.strict()) {
			output.append(indent).append("@strict").append(LINE_SEPARATOR);
		}

		if (groupDefinition.format() != null) {
			output.append(indent).append("@format ");
			writeLowerCaseEnum(groupDefinition.format());
			output.append(LINE_SEPARATOR);
		}

		for (Expression constant : groupDefinition.constants()) {
			output.append(indent);
			constant.accept(new ExpressionWriter());
			output.append(LINE_SEPARATOR);
		}
	}

	private void writeGroupScope(GroupScope scope) {
		output.append("@scope ");
		StringBuilder ignored = switch (scope) {
			case GroupScope.Package(String packageName) -> output.append("package ").append(packageName);
			case GroupScope.Class(String className) -> output.append("class ").append(className);
			case GroupScope.Method(String className, String methodName, String methodDesc) -> output.append("method ")
					.append(className)
					.append(" ")
					.append(methodName)
					.append(" ")
					.append(methodDesc);
		};
	}

	@Override
	public void visitTargetField(TargetField targetField) {
		output.append(LINE_SEPARATOR)
				.append("target_field ")
				.append(targetField.className())
				.append(" ")
				.append(targetField.fieldName())
				.append(" ")
				.append(targetField.fieldDesc())
				.append(" ")
				.append(targetField.groupName())
				.append(LINE_SEPARATOR);
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod) {
		output.append(LINE_SEPARATOR)
				.append("target_method ")
				.append(targetMethod.className())
				.append(" ")
				.append(targetMethod.methodName())
				.append(" ")
				.append(targetMethod.methodDesc())
				.append(LINE_SEPARATOR);

		List<Map.Entry<Integer, String>> paramGroups = new ArrayList<>(targetMethod.paramGroups().entrySet());
		paramGroups.sort(Map.Entry.comparingByKey());
		for (Map.Entry<Integer, String> paramGroup : paramGroups) {
			output.append(indent)
					.append("param ")
					.append(paramGroup.getKey())
					.append(" ")
					.append(paramGroup.getValue())
					.append(LINE_SEPARATOR);
		}

		if (targetMethod.returnGroup() != null) {
			output.append(indent)
					.append("return ")
					.append(targetMethod.returnGroup())
					.append(LINE_SEPARATOR);
		}
	}

	private void writeRadixPrefix(int radix) {
		switch (radix) {
			case 10 -> {
			}
			case 16 -> output.append("0x");
			case 8 -> output.append("0");
			case 2 -> output.append("0b");
			default -> throw new AssertionError("Illegal radix: " + radix);
		}
	}

	private void writeDataType(DataType dataType) {
		switch (dataType) {
			case STRING -> output.append("String");
			case CLASS -> output.append("Class");
			default -> writeLowerCaseEnum(dataType);
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
				case '\b' -> result.append("\\b");
				case '\t' -> result.append("\\t");
				case '\n' -> result.append("\\n");
				case '\f' -> result.append("\\f");
				case '\r' -> result.append("\\r");
				case '\\' -> result.append("\\\\");
				default -> {
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
		}

		return result.append(quoteChar).toString();
	}

	private static boolean isPrintable(char ch) {
		return switch (Character.getType(ch)) {
			case Character.UPPERCASE_LETTER,
				Character.LOWERCASE_LETTER,
				Character.TITLECASE_LETTER,
				Character.MODIFIER_LETTER,
				Character.OTHER_LETTER,
				Character.NON_SPACING_MARK,
				Character.ENCLOSING_MARK,
				Character.COMBINING_SPACING_MARK,
				Character.DECIMAL_DIGIT_NUMBER,
				Character.LETTER_NUMBER,
				Character.OTHER_NUMBER,
				Character.SPACE_SEPARATOR,
				Character.DASH_PUNCTUATION,
				Character.START_PUNCTUATION,
				Character.END_PUNCTUATION,
				Character.CONNECTOR_PUNCTUATION,
				Character.OTHER_PUNCTUATION,
				Character.MATH_SYMBOL,
				Character.CURRENCY_SYMBOL,
				Character.MODIFIER_SYMBOL,
				Character.OTHER_SYMBOL,
				Character.INITIAL_QUOTE_PUNCTUATION,
				Character.FINAL_QUOTE_PUNCTUATION -> true;
			default -> false;
		};
	}

	public String getOutput() {
		return output.toString();
	}

	private final class ExpressionWriter extends ExpressionVisitor {
		@Override
		public void visitBinaryExpression(BinaryExpression binaryExpression) {
			binaryExpression.lhs.accept(this);
			String operatorStr = switch (binaryExpression.operator) {
				case BIT_OR -> " | ";
				case BIT_XOR -> " ^ ";
				case BIT_AND -> " & ";
				case BIT_SHIFT_LEFT -> " << ";
				case BIT_SHIFT_RIGHT -> " >> ";
				case BIT_SHIFT_RIGHT_UNSIGNED -> " >>> ";
				case ADD -> " + ";
				case SUBTRACT -> " - ";
				case MULTIPLY -> " * ";
				case DIVIDE -> " / ";
				case MODULO -> " % ";
			};
			output.append(operatorStr);
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
			output.append(fieldExpression.className).append('.');
			if (fieldExpression.fieldName == null) {
				output.append('*');
			} else {
				output.append(fieldExpression.fieldName);
			}
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
			StringBuilder ignored = switch (literalExpression.literal) {
				case Literal.Integer(int value, int radix) -> {
					writeRadixPrefix(radix);
					yield output.append(Integer.toUnsignedString(value, radix));
				}
				case Literal.Long(long value, int radix) -> {
					writeRadixPrefix(radix);
					yield output.append(Long.toUnsignedString(value, radix)).append('L');
				}
				case Literal.Float(float value) -> output.append(value).append('F');
				case Literal.Double(double value) -> output.append(value);
				case Literal.Character(char value) -> output.append(quoteString(String.valueOf(value), '\''));
				case Literal.String(String value) -> output.append(quoteString(value, '"'));
			};
		}

		@Override
		public void visitParenExpression(ParenExpression parenExpression) {
			output.append('(');
			parenExpression.expression.accept(this);
			output.append(')');
		}

		@Override
		public void visitUnaryExpression(UnaryExpression unaryExpression) {
			char operatorChar = switch (unaryExpression.operator) {
				case NEGATE -> '-';
				case BIT_NOT -> '~';
			};
			output.append(operatorChar);
			unaryExpression.operand.accept(this);
		}
	}
}

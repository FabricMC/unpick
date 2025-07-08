package daomephsta.unpick.constantmappers.datadriven.parser.v3;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupFormat;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.BinaryExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.CastExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.LiteralExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ParenExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.UnaryExpression;

/**
 * Performs syntax checking and basic semantic checking on .unpick v3 format text, and allows its structure to be
 * visited by instances of {@link UnpickV3Visitor}.
 */
public final class UnpickV3Reader implements AutoCloseable {
	private static final int MAX_PARSE_DEPTH = 64;
	private static final EnumMap<BinaryExpression.Operator, Integer> PRECEDENCES = new EnumMap<>(BinaryExpression.Operator.class);
	static {
		PRECEDENCES.put(BinaryExpression.Operator.BIT_OR, 0);
		PRECEDENCES.put(BinaryExpression.Operator.BIT_XOR, 1);
		PRECEDENCES.put(BinaryExpression.Operator.BIT_AND, 2);
		PRECEDENCES.put(BinaryExpression.Operator.BIT_SHIFT_LEFT, 3);
		PRECEDENCES.put(BinaryExpression.Operator.BIT_SHIFT_RIGHT, 3);
		PRECEDENCES.put(BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED, 3);
		PRECEDENCES.put(BinaryExpression.Operator.ADD, 4);
		PRECEDENCES.put(BinaryExpression.Operator.SUBTRACT, 4);
		PRECEDENCES.put(BinaryExpression.Operator.MULTIPLY, 5);
		PRECEDENCES.put(BinaryExpression.Operator.DIVIDE, 5);
		PRECEDENCES.put(BinaryExpression.Operator.MODULO, 5);
	}

	private final LineNumberReader reader;
	private String line;
	private int column;
	private int lastTokenLine;
	private int lastTokenColumn;
	private TokenType lastTokenType;
	@Nullable
	private String lastDocs;
	private String nextToken;
	private ParseState nextTokenState;
	private String nextToken2;
	private ParseState nextToken2State;

	public UnpickV3Reader(Reader reader) {
		this.reader = new LineNumberReader(reader);
	}

	public void accept(UnpickV3Visitor visitor) throws IOException {
		line = reader.readLine();
		if (!"unpick v3".equals(line)) {
			throw parseError("Missing version marker", 1, 0);
		}
		column = line.length();

		nextToken(); // newline

		while (true) {
			String token = nextToken();
			if (lastTokenType == TokenType.EOF) {
				break;
			}
			parseUnpickItem(visitor, token);
		}
	}

	private void parseUnpickItem(UnpickV3Visitor visitor, String token) throws IOException {
		if (lastTokenType != TokenType.IDENTIFIER) {
			throw expectedTokenError("unpick item", token);
		}

		switch (token) {
			case "target_field" -> visitor.visitTargetField(parseTargetField());
			case "target_method" -> visitor.visitTargetMethod(parseTargetMethod());
			case "group" -> visitor.visitGroupDefinition(parseGroupDefinition());
			default -> throw expectedTokenError("unpick item", token);
		}
	}

	private GroupScope parseGroupScope() throws IOException {
		String token = nextToken("group scope type", TokenType.IDENTIFIER);
		return switch (token) {
			case "package" -> new GroupScope.Package(parseClassName("package name"));
			case "class" -> new GroupScope.Class(parseClassName());
			case "method" -> {
				String className = parseClassName();
				String methodName = parseMethodName();
				String methodDesc = nextToken(TokenType.METHOD_DESCRIPTOR);
				yield new GroupScope.Method(className, methodName, methodDesc);
			}
			default -> throw expectedTokenError("group scope type", token);
		};
	}

	private GroupDefinition parseGroupDefinition() throws IOException {
		String docs = lastDocs;
		DataType dataType = parseDataType();
		if (!isDataTypeValidInGroup(dataType)) {
			throw parseError("Data type not allowed in group: " + dataType);
		}

		String name = peekTokenType() == TokenType.IDENTIFIER ? nextToken() : null;

		List<GroupScope> scopes = new ArrayList<>();
		boolean flags = false;
		boolean strict = false;
		List<Expression> constants = new ArrayList<>();
		GroupFormat format = null;

		boolean finishedAttributes = false;

		while (true) {
			String token = nextToken();
			if (lastTokenType == TokenType.EOF) {
				break;
			}
			if (lastTokenType != TokenType.NEWLINE) {
				throw expectedTokenError("'\\n'", token);
			}

			if (peekTokenType() != TokenType.INDENT) {
				break;
			}
			nextToken();

			if ("@".equals(peekToken())) {
				nextToken();
				if (finishedAttributes) {
					throw parseError("Found attribute after expression");
				}
				token = nextToken("attribute name", TokenType.IDENTIFIER);
				switch (token) {
					case "scope" -> scopes.add(parseGroupScope());
					case "flags" -> {
						if (flags) {
							throw parseError("Duplicate flags attribute");
						}
						if (dataType != DataType.INT && dataType != DataType.LONG) {
							throw parseError("The flags attribute is not applicable to this data type");
						}
						if (name == null) {
							throw parseError("The flags attribute is not applicable to the default group");
						}
						flags = true;
					}
					case "strict" -> {
						if (strict) {
							throw parseError("Duplicate strict attribute");
						}
						strict = true;
					}
					case "format" -> {
						if (format != null) {
							throw parseError("Duplicate format attribute");
						}
						if (dataType != DataType.INT && dataType != DataType.LONG && dataType != DataType.FLOAT && dataType != DataType.DOUBLE) {
							throw parseError("The format attribute is not applicable to this data type");
						}
						format = parseGroupFormat();
						if (format != GroupFormat.DECIMAL && format != GroupFormat.HEX && dataType != DataType.INT && dataType != DataType.LONG) {
							throw parseError("This format is not applicable to floating point data types");
						}
					}
					default -> throw expectedTokenError("attribute name", token);
				}
			} else {
				finishedAttributes = true;
				constants.add(parseExpression(0));
			}
		}

		return new GroupDefinition(scopes, flags, strict, dataType, name, constants, format, docs);
	}

	private static boolean isDataTypeValidInGroup(DataType type) {
		return type == DataType.INT || type == DataType.LONG || type == DataType.FLOAT || type == DataType.DOUBLE || type == DataType.STRING || type == DataType.CLASS;
	}

	private GroupFormat parseGroupFormat() throws IOException {
		String token = nextToken("group format", TokenType.IDENTIFIER);
		return switch (token) {
			case "decimal" -> GroupFormat.DECIMAL;
			case "hex" -> GroupFormat.HEX;
			case "binary" -> GroupFormat.BINARY;
			case "octal" -> GroupFormat.OCTAL;
			case "char" -> GroupFormat.CHAR;
			default -> throw expectedTokenError("group format", token);
		};
	}

	private Expression parseExpression(int parseDepth) throws IOException {
		// Shunting yard algorithm for parsing with operator precedence: https://stackoverflow.com/a/47717/11071180
		Stack<Expression> operandStack = new Stack<>();
		Stack<BinaryExpression.Operator> operatorStack = new Stack<>();

		operandStack.push(parseUnaryExpression(parseDepth, false));

		while (true) {
			BinaryExpression.Operator operator = switch (peekToken()) {
				case "|" -> BinaryExpression.Operator.BIT_OR;
				case "^" -> BinaryExpression.Operator.BIT_XOR;
				case "&" -> BinaryExpression.Operator.BIT_AND;
				case "<<" -> BinaryExpression.Operator.BIT_SHIFT_LEFT;
				case ">>" -> BinaryExpression.Operator.BIT_SHIFT_RIGHT;
				case ">>>" -> BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED;
				case "+" -> BinaryExpression.Operator.ADD;
				case "-" -> BinaryExpression.Operator.SUBTRACT;
				case "*" -> BinaryExpression.Operator.MULTIPLY;
				case "/" -> BinaryExpression.Operator.DIVIDE;
				case "%" -> BinaryExpression.Operator.MODULO;
				default -> null;
			};
			if (operator == null) {
				break;
			}
			nextToken(); // consume the operator

			int ourPrecedence = PRECEDENCES.get(operator);
			while (!operatorStack.isEmpty() && ourPrecedence <= PRECEDENCES.get(operatorStack.peek())) {
				BinaryExpression.Operator op = operatorStack.pop();
				Expression rhs = operandStack.pop();
				Expression lhs = operandStack.pop();
				operandStack.push(new BinaryExpression(lhs, rhs, op));
			}

			operatorStack.push(operator);
			operandStack.push(parseUnaryExpression(parseDepth, false));
		}

		Expression result = operandStack.pop();
		while (!operatorStack.isEmpty()) {
			result = new BinaryExpression(operandStack.pop(), result, operatorStack.pop());
		}

		return result;
	}

	private Expression parseUnaryExpression(int parseDepth, boolean negative) throws IOException {
		if (parseDepth > MAX_PARSE_DEPTH) {
			throw parseError("max parse depth reached");
		}

		String token = nextToken();
		switch (token) {
			case "-" -> {
				return new UnaryExpression(parseUnaryExpression(parseDepth + 1, true), UnaryExpression.Operator.NEGATE);
			}
			case "~" -> {
				return new UnaryExpression(parseUnaryExpression(parseDepth + 1, false), UnaryExpression.Operator.BIT_NOT);
			}
			case "(" -> {
				boolean parseAsCast = peekTokenType() == TokenType.IDENTIFIER && ")".equals(peekToken2());
				if (parseAsCast) {
					DataType castType = parseDataType();
					nextToken(); // close paren
					return new CastExpression(castType, parseUnaryExpression(parseDepth + 1, false));
				} else {
					Expression expression = parseExpression(parseDepth + 1);
					expectToken(")");
					return new ParenExpression(expression);
				}
			}
		}

		return switch (lastTokenType) {
			case IDENTIFIER -> parseFieldExpression(token);
			case INTEGER -> {
				ParsedInteger parsedInt = parseInt(token, negative);
				yield new LiteralExpression(new Literal.Integer(negative ? -parsedInt.value : parsedInt.value, parsedInt.radix));
			}
			case LONG -> {
				ParsedLong parsedLong = parseLong(token, negative);
				yield new LiteralExpression(new Literal.Long(negative ? -parsedLong.value : parsedLong.value, parsedLong.radix));
			}
			case FLOAT -> {
				float parsedFloat = parseFloat(token, negative);
				yield new LiteralExpression(new Literal.Float(negative ? -parsedFloat : parsedFloat));
			}
			case DOUBLE -> {
				double parsedDouble = parseDouble(token, negative);
				yield new LiteralExpression(new Literal.Double(negative ? -parsedDouble : parsedDouble));
			}
			case CHAR -> new LiteralExpression(new Literal.Character(unquoteChar(token)));
			case STRING -> new LiteralExpression(new Literal.String(unquoteString(token)));
			default -> throw expectedTokenError("expression", token);
		};
	}

	private FieldExpression parseFieldExpression(String token) throws IOException {
		expectToken(".");
		StringBuilder classAndFieldNameBuilder = new StringBuilder(token).append('.');
		while (true) {
			if ("*".equals(peekToken())) {
				nextToken();
				classAndFieldNameBuilder.append('*');
				break;
			}
			classAndFieldNameBuilder.append(nextToken(TokenType.IDENTIFIER));
			if (!".".equals(peekToken())) {
				break;
			}
			nextToken();
			classAndFieldNameBuilder.append('.');
		}
		String classAndFieldName = classAndFieldNameBuilder.toString();

		// the field name has been joined to the class name, split it off
		int dotIndex = classAndFieldName.lastIndexOf('.');
		String className = classAndFieldName.substring(0, dotIndex);
		String fieldName = classAndFieldName.substring(dotIndex + 1);
		if ("*".equals(fieldName)) {
			fieldName = null;
		}

		boolean isStatic = true;
		DataType fieldType = null;
		if (":".equals(peekToken())) {
			nextToken();
			if ("instance".equals(peekToken())) {
				nextToken();
				isStatic = false;
				if (":".equals(peekToken())) {
					nextToken();
					fieldType = parseDataType();
				}
			} else {
				fieldType = parseDataType();
			}
		}

		return new FieldExpression(className, fieldName, fieldType, isStatic);
	}

	private TargetField parseTargetField() throws IOException {
		String className = parseClassName();
		String fieldName = nextToken(TokenType.IDENTIFIER);
		String fieldDesc = nextToken(TokenType.TYPE_DESCRIPTOR);
		String groupName = nextToken(TokenType.IDENTIFIER);
		String token = nextToken();
		if (lastTokenType != TokenType.NEWLINE && lastTokenType != TokenType.EOF) {
			throw expectedTokenError("'\n'", token);
		}
		return new TargetField(className, fieldName, fieldDesc, groupName);
	}

	private TargetMethod parseTargetMethod() throws IOException {
		String className = parseClassName();
		String methodName = parseMethodName();
		String methodDesc = nextToken(TokenType.METHOD_DESCRIPTOR);

		Map<Integer, String> paramGroups = new HashMap<>();
		String returnGroup = null;

		while (true) {
			String token = nextToken();
			if (lastTokenType == TokenType.EOF) {
				break;
			}
			if (lastTokenType != TokenType.NEWLINE) {
				throw expectedTokenError("'\\n'", token);
			}

			if (peekTokenType() != TokenType.INDENT) {
				break;
			}
			nextToken();

			token = nextToken("target method item", TokenType.IDENTIFIER);
			switch (token) {
				case "param" -> {
					int paramIndex = parseInt(nextToken(TokenType.INTEGER), false).value;
					if (paramGroups.containsKey(paramIndex)) {
						throw parseError("Specified parameter " + paramIndex + " twice");
					}
					paramGroups.put(paramIndex, nextToken(TokenType.IDENTIFIER));
				}
				case "return" -> {
					if (returnGroup != null) {
						throw parseError("Specified return group twice");
					}
					returnGroup = nextToken(TokenType.IDENTIFIER);
				}
				default -> throw expectedTokenError("target method item", token);
			}
		}

		return new TargetMethod(className, methodName, methodDesc, paramGroups, returnGroup);
	}

	private DataType parseDataType() throws IOException {
		String token = nextToken("data type", TokenType.IDENTIFIER);
		return switch (token) {
			case "byte" -> DataType.BYTE;
			case "short" -> DataType.SHORT;
			case "int" -> DataType.INT;
			case "long" -> DataType.LONG;
			case "float" -> DataType.FLOAT;
			case "double" -> DataType.DOUBLE;
			case "char" -> DataType.CHAR;
			case "String" -> DataType.STRING;
			case "Class" -> DataType.CLASS;
			default -> throw expectedTokenError("data type", token);
		};
	}

	private String parseClassName() throws IOException {
		return parseClassName("class name");
	}

	private String parseClassName(String expected) throws IOException {
		StringBuilder result = new StringBuilder(nextToken(expected, TokenType.IDENTIFIER));
		while (".".equals(peekToken())) {
			nextToken();
			result.append('.').append(nextToken(TokenType.IDENTIFIER));
		}
		return result.toString();
	}

	private String parseMethodName() throws IOException {
		String token = nextToken();
		if (lastTokenType == TokenType.IDENTIFIER) {
			return token;
		}
		if ("<".equals(token)) {
			token = nextToken(TokenType.IDENTIFIER);
			if (!"init".equals(token) && !"clinit".equals(token)) {
				throw expectedTokenError("identifier", token);
			}
			expectToken(">");
			return "<" + token + ">";
		}
		throw expectedTokenError("identifier", token);
	}

	private ParsedInteger parseInt(String string, boolean negative) throws UnpickSyntaxException {
		int radix;
		if (string.startsWith("0x") || string.startsWith("0X")) {
			radix = 16;
			string = string.substring(2);
		} else if (string.startsWith("0b") || string.startsWith("0B")) {
			radix = 2;
			string = string.substring(2);
		} else if (string.startsWith("0") && string.length() > 1) {
			radix = 8;
			string = string.substring(1);
		} else {
			radix = 10;
		}

		try {
			return new ParsedInteger(Integer.parseInt(negative ? "-" + string : string, radix), radix);
		} catch (NumberFormatException ignore) {
		}

		// try unsigned parsing in other radixes
		if (!negative && radix != 10) {
			try {
				return new ParsedInteger(Integer.parseUnsignedInt(string, radix), radix);
			} catch (NumberFormatException ignore) {
			}
		}

		throw parseError("Integer out of bounds");
	}

	private record ParsedInteger(int value, int radix) {
	}

	private ParsedLong parseLong(String string, boolean negative) throws UnpickSyntaxException {
		if (string.endsWith("l") || string.endsWith("L")) {
			string = string.substring(0, string.length() - 1);
		}

		int radix;
		if (string.startsWith("0x") || string.startsWith("0X")) {
			radix = 16;
			string = string.substring(2);
		} else if (string.startsWith("0b") || string.startsWith("0B")) {
			radix = 2;
			string = string.substring(2);
		} else if (string.startsWith("0") && string.length() > 1) {
			radix = 8;
			string = string.substring(1);
		} else {
			radix = 10;
		}

		try {
			return new ParsedLong(Long.parseLong(negative ? "-" + string : string, radix), radix);
		} catch (NumberFormatException ignore) {
		}

		// try unsigned parsing in other radixes
		if (!negative && radix != 10) {
			try {
				return new ParsedLong(Long.parseUnsignedLong(string, radix), radix);
			} catch (NumberFormatException ignore) {
			}
		}

		throw parseError("Long out of bounds");
	}

	private record ParsedLong(long value, int radix) {
	}

	private float parseFloat(String string, boolean negative) throws UnpickSyntaxException {
		if (string.endsWith("f") || string.endsWith("F")) {
			string = string.substring(0, string.length() - 1);
		}
		try {
			float result = Float.parseFloat(string);
			if (!Float.isFinite(result)) {
				throw parseError("Float out of bounds");
			}
			return negative ? -result : result;
		} catch (NumberFormatException e) {
			throw parseError("Invalid float");
		}
	}

	private double parseDouble(String string, boolean negative) throws UnpickSyntaxException {
		try {
			double result = Double.parseDouble(string);
			if (!Double.isFinite(result)) {
				throw parseError("Double out of bounds");
			}
			return negative ? -result : result;
		} catch (NumberFormatException e) {
			throw parseError("Invalid double");
		}
	}

	private static char unquoteChar(String string) {
		return unquoteString(string).charAt(0);
	}

	private static String unquoteString(String string) {
		StringBuilder result = new StringBuilder(string.length() - 2);
		for (int i = 1; i < string.length() - 1; i++) {
			if (string.charAt(i) == '\\') {
				i++;
				switch (string.charAt(i)) {
					case 'u' -> {
						do {
							i++;
						} while (string.charAt(i) == 'u');
						result.append((char) Integer.parseInt(string.substring(i, i + 4), 16));
						i += 3;
					}
					case 'b' -> result.append('\b');
					case 't' -> result.append('\t');
					case 'n' -> result.append('\n');
					case 'f' -> result.append('\f');
					case 'r' -> result.append('\r');
					case '"' -> result.append('"');
					case '\'' -> result.append('\'');
					case '\\' -> result.append('\\');
					case '0', '1', '2', '3', '4', '5', '6', '7' -> {
						char c;
						int count = 0;
						int maxCount = string.charAt(i) <= '3' ? 3 : 2;
						while (count < maxCount && (c = string.charAt(i + count)) >= '0' && c <= '7') {
							count++;
						}
						result.append((char) Integer.parseInt(string.substring(i, i + count), 8));
						i += count - 1;
					}
					default -> throw new AssertionError("Unexpected escape sequence in string");
				}
			} else {
				result.append(string.charAt(i));
			}
		}
		return result.toString();
	}

	// region Tokenizer

	private TokenType peekTokenType() throws IOException {
		ParseState state = new ParseState(this);
		nextToken = nextToken();
		nextTokenState = new ParseState(this);
		state.restore(this);
		return nextTokenState.lastTokenType;
	}

	private String peekToken() throws IOException {
		ParseState state = new ParseState(this);
		nextToken = nextToken();
		nextTokenState = new ParseState(this);
		state.restore(this);
		return nextToken;
	}

	private String peekToken2() throws IOException {
		ParseState state = new ParseState(this);
		String nextToken = nextToken();
		ParseState nextTokenState = new ParseState(this);
		nextToken2 = nextToken();
		nextToken2State = new ParseState(this);
		this.nextToken = nextToken;
		this.nextTokenState = nextTokenState;
		state.restore(this);
		return nextToken2;
	}

	private void expectToken(String expected) throws IOException {
		String token = nextToken();
		if (!expected.equals(token)) {
			throw expectedTokenError(UnpickV3Writer.quoteString(expected, '\''), token);
		}
	}

	private String nextToken() throws IOException {
		return nextTokenInner(null);
	}

	private String nextToken(TokenType type) throws IOException {
		return nextToken(type.name, type);
	}

	private String nextToken(String expected, TokenType type) throws IOException {
		String token = nextTokenInner(type);
		if (lastTokenType != type) {
			throw expectedTokenError(expected, token);
		}
		return token;
	}

	private String nextTokenInner(@Nullable TokenType typeHint) throws IOException {
		if (nextTokenState != null) {
			String tok = nextToken;
			nextToken = nextToken2;
			nextToken2 = null;
			nextTokenState.restore(this);
			nextTokenState = nextToken2State;
			nextToken2State = null;
			return tok;
		}

		if (lastTokenType == TokenType.EOF) {
			return null;
		}

		// start doc comment anew if the previous token type isn't whitespace
		if (lastTokenType != TokenType.NEWLINE && lastTokenType != TokenType.INDENT) {
			lastDocs = null;
		}

		// newline token (skipping comment and whitespace)
		while (column < line.length() && Character.isWhitespace(line.charAt(column))) {
			column++;
		}
		processCommentIfPresent();
		if (column == line.length() && lastTokenType != TokenType.NEWLINE) {
			lastTokenColumn = column;
			lastTokenLine = reader.getLineNumber();
			lastTokenType = TokenType.NEWLINE;
			return "\n";
		}

		// skip whitespace and comments, handle indent token
		boolean seenIndent = false;
		while (true) {
			processCommentIfPresent();
			if (column == line.length()) {
				seenIndent = false;
				line = reader.readLine();
				column = 0;
				if (line == null) {
					lastTokenColumn = column;
					lastTokenLine = reader.getLineNumber();
					lastTokenType = TokenType.EOF;
					return null;
				}
			} else if (Character.isWhitespace(line.charAt(column))) {
				seenIndent = column == 0;
				do {
					column++;
				} while (column < line.length() && Character.isWhitespace(line.charAt(column)));
			} else {
				break;
			}
		}
		if (seenIndent) {
			lastTokenColumn = 0;
			lastTokenLine = reader.getLineNumber();
			lastTokenType = TokenType.INDENT;
			return line.substring(0, column);
		}

		lastTokenColumn = column;
		lastTokenLine = reader.getLineNumber();

		if (typeHint == TokenType.TYPE_DESCRIPTOR) {
			if (skipFieldDescriptor(true)) {
				return line.substring(lastTokenColumn, column);
			}
		}

		if (typeHint == TokenType.METHOD_DESCRIPTOR) {
			if (skipMethodDescriptor()) {
				return line.substring(lastTokenColumn, column);
			}
		}

		if (skipNumber()) {
			if (column < line.length() && isIdentifierChar(line.charAt(column))) {
				throw parseErrorInToken("Unexpected character in number: " + line.charAt(column));
			}
			return line.substring(lastTokenColumn, column);
		}

		if (skipIdentifier()) {
			return line.substring(lastTokenColumn, column);
		}

		if (skipString('\'', true)) {
			lastTokenType = TokenType.CHAR;
			return line.substring(lastTokenColumn, column);
		}

		if (skipString('"', false)) {
			lastTokenType = TokenType.STRING;
			return line.substring(lastTokenColumn, column);
		}

		char c = line.charAt(column);
		column++;
		if (c == '<') {
			if (column < line.length() && line.charAt(column) == '<') {
				column++;
			}
		} else if (c == '>') {
			if (column < line.length() && line.charAt(column) == '>') {
				column++;
				if (column < line.length() && line.charAt(column) == '>') {
					column++;
				}
			}
		}

		lastTokenType = TokenType.OPERATOR;
		return line.substring(lastTokenColumn, column);
	}

	private void processCommentIfPresent() {
		if (column >= line.length() || line.charAt(column) != '#') {
			return;
		}
		column++;

		// handle doc comments
		if (column < line.length() && line.charAt(column) == ':') {
			do {
				column++;
			} while (column < line.length() && Character.isWhitespace(line.charAt(column)));
			if (lastDocs == null) {
				lastDocs = "";
			} else {
				lastDocs += "\n";
			}
			lastDocs += line.substring(column);
		} else {
			lastDocs = null;
		}

		column = line.length();
	}

	private boolean skipFieldDescriptor(boolean startOfToken) throws UnpickSyntaxException {
		// array descriptors
		while (column < line.length() && line.charAt(column) == '[') {
			startOfToken = false;
			column++;
		}

		// first character of main part of descriptor
		if (column == line.length() || isTokenEnd(line.charAt(column))) {
			throw parseErrorInToken("Unexpected end of descriptor");
		}
		switch (line.charAt(column)) {
			// primitive types
			case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> column++;

			// class types
			case 'L' -> {
				column++;

				// class name
				char c;
				while (column < line.length() && (c = line.charAt(column)) != ';' && !isTokenEnd(c)) {
					if (c == '.' || c == '[') {
						throw parseErrorInToken("Illegal character in descriptor: " + c);
					}
					column++;
				}

				// semicolon
				if (column == line.length() || isTokenEnd(line.charAt(column))) {
					throw parseErrorInToken("Unexpected end of descriptor");
				}
				column++;
			}
			default -> {
				if (!startOfToken) {
					throw parseErrorInToken("Illegal character in descriptor: " + line.charAt(column));
				}
				return false;
			}
		}

		lastTokenType = TokenType.TYPE_DESCRIPTOR;
		return true;
	}

	private boolean skipMethodDescriptor() throws UnpickSyntaxException {
		if (line.charAt(column) != '(') {
			return false;
		}
		column++;

		// parameter types
		while (column < line.length() && line.charAt(column) != ')' && !isTokenEnd(line.charAt(column))) {
			skipFieldDescriptor(false);
		}
		if (column == line.length() || isTokenEnd(line.charAt(column))) {
			throw parseErrorInToken("Unexpected end of descriptor");
		}
		column++;

		// return type
		if (column == line.length() || isTokenEnd(line.charAt(column))) {
			throw parseErrorInToken("Unexpected end of descriptor");
		}
		if (line.charAt(column) == 'V') {
			column++;
		} else {
			skipFieldDescriptor(false);
		}

		lastTokenType = TokenType.METHOD_DESCRIPTOR;
		return true;
	}

	private boolean skipNumber() throws UnpickSyntaxException {
		if (line.charAt(column) < '0' || line.charAt(column) > '9') {
			return false;
		}

		// hex numbers
		if (line.startsWith("0x", column) || line.startsWith("0X", column)) {
			column += 2;
			char c;
			boolean seenDigit = false;
			while (column < line.length() && ((c = line.charAt(column)) >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')) {
				seenDigit = true;
				column++;
			}
			if (!seenDigit) {
				throw parseErrorInToken("Unexpected end of integer");
			}
			detectIntegerType();
			return true;
		}

		// binary numbers
		if (line.startsWith("0b", column) || line.startsWith("0B", column)) {
			column += 2;
			char c;
			boolean seenDigit = false;
			while (column < line.length() && ((c = line.charAt(column)) == '0' || c == '1')) {
				seenDigit = true;
				column++;
			}
			if (!seenDigit) {
				throw parseErrorInToken("Unexpected end of integer");
			}
			detectIntegerType();
			return true;
		}

		// lookahead a decimal number
		int endOfInteger = column;
		char c;
		do {
			endOfInteger++;
		} while (endOfInteger < line.length() && (c = line.charAt(endOfInteger)) >= '0' && c <= '9');

		// floats and doubles
		if (endOfInteger < line.length() && line.charAt(endOfInteger) == '.') {
			column = endOfInteger + 1;

			// fractional part
			boolean seenFracDigit = false;
			while (column < line.length() && (c = line.charAt(column)) >= '0' && c <= '9') {
				seenFracDigit = true;
				column++;
			}
			if (!seenFracDigit) {
				throw parseErrorInToken("Unexpected end of float");
			}

			// exponent
			if (column < line.length() && ((c = line.charAt(column)) == 'e' || c == 'E')) {
				column++;
				if (column < line.length() && (c = line.charAt(column)) >= '+' && c <= '-') {
					column++;
				}

				boolean seenExponentDigit = false;
				while (column < line.length() && ((c = line.charAt(column)) >= '0' && c <= '9')) {
					seenExponentDigit = true;
					column++;
				}
				if (!seenExponentDigit) {
					throw parseErrorInToken("Unexpected end of float");
				}
			}

			boolean isFloat = column < line.length() && ((c = line.charAt(column)) == 'f' || c == 'F');
			if (isFloat) {
				column++;
			}
			lastTokenType = isFloat ? TokenType.FLOAT : TokenType.DOUBLE;
			return true;
		}

		// octal numbers (we'll count 0 itself as an octal)
		if (line.charAt(column) == '0') {
			column++;
			while (column < line.length() && (c = line.charAt(column)) >= '0' && c <= '7') {
				column++;
			}
			detectIntegerType();
			return true;
		}

		// decimal numbers
		column = endOfInteger;
		detectIntegerType();
		return true;
	}

	private void detectIntegerType() {
		char c;
		boolean isLong = column < line.length() && ((c = line.charAt(column)) == 'l' || c == 'L');
		if (isLong) {
			column++;
		}
		lastTokenType = isLong ? TokenType.LONG : TokenType.INTEGER;
	}

	private boolean skipIdentifier() {
		if (!isIdentifierChar(line.charAt(column))) {
			return false;
		}

		do {
			column++;
		} while (column < line.length() && isIdentifierChar(line.charAt(column)));

		lastTokenType = TokenType.IDENTIFIER;
		return true;
	}

	private boolean skipString(char quoteChar, boolean singleChar) throws UnpickSyntaxException {
		if (line.charAt(column) != quoteChar) {
			return false;
		}
		column++;

		boolean seenChar = false;
		while (column < line.length() && line.charAt(column) != quoteChar) {
			if (singleChar && seenChar) {
				throw parseErrorInToken("Multiple characters in char literal");
			}
			seenChar = true;

			if (line.charAt(column) == '\\') {
				column++;
				if (column == line.length()) {
					throw parseErrorInToken("Unexpected end of string");
				}
				char c = line.charAt(column);
				switch (c) {
					case 'u' -> {
						do {
							column++;
						} while (column < line.length() && line.charAt(column) == 'u');
						for (int i = 0; i < 4; i++) {
							if (column == line.length()) {
								throw parseErrorInToken("Unexpected end of string");
							}
							c = line.charAt(column);
							if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
								throw parseErrorInToken("Illegal character in unicode escape sequence");
							}
							column++;
						}
					}
					case 'b', 't', 'n', 'f', 'r', '"', '\'', '\\' -> column++;
					case '0', '1', '2', '3', '4', '5', '6', '7' -> {
						column++;
						int maxOctalDigits = c <= '3' ? 3 : 2;
						for (int i = 1; i < maxOctalDigits && column < line.length() && (c = line.charAt(column)) >= '0' && c <= '7'; i++) {
							column++;
						}
					}
					default -> throw parseErrorInToken("Illegal escape sequence \\" + c);
				}
			} else {
				column++;
			}
		}

		if (column == line.length()) {
			throw parseErrorInToken("Unexpected end of string");
		}

		if (singleChar && !seenChar) {
			throw parseErrorInToken("No character in char literal");
		}

		column++;
		return true;
	}

	private static boolean isTokenEnd(char c) {
		return Character.isWhitespace(c) || c == '#';
	}

	private static boolean isIdentifierChar(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '$';
	}

	// endregion

	private UnpickSyntaxException expectedTokenError(String expected, String token) {
		if (lastTokenType == TokenType.EOF) {
			return parseError("Expected " + expected + " before eof token");
		} else {
			return parseError("Expected " + expected + " before " + UnpickV3Writer.quoteString(token, '\'') + " token");
		}
	}

	private UnpickSyntaxException parseError(String message) {
		return parseError(message, lastTokenLine, lastTokenColumn);
	}

	private UnpickSyntaxException parseErrorInToken(String message) {
		return parseError(message, reader.getLineNumber(), column);
	}

	private UnpickSyntaxException parseError(String message, int lineNumber, int column) {
		return new UnpickSyntaxException(lineNumber, column + 1, message);
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	private static class ParseState {
		private final int lastTokenLine;
		private final int lastTokenColumn;
		private final TokenType lastTokenType;
		@Nullable
		private final String lastDocs;

		ParseState(UnpickV3Reader reader) {
			this.lastTokenLine = reader.lastTokenLine;
			this.lastTokenColumn = reader.lastTokenColumn;
			this.lastTokenType = reader.lastTokenType;
			this.lastDocs = reader.lastDocs;
		}

		void restore(UnpickV3Reader reader) {
			reader.lastTokenLine = lastTokenLine;
			reader.lastTokenColumn = lastTokenColumn;
			reader.lastTokenType = lastTokenType;
			reader.lastDocs = lastDocs;
		}
	}

	private enum TokenType {
		IDENTIFIER("identifier"),
		DOUBLE("double"),
		FLOAT("float"),
		INTEGER("integer"),
		LONG("long"),
		CHAR("char"),
		STRING("string"),
		INDENT("indent"),
		NEWLINE("newline"),
		TYPE_DESCRIPTOR("type descriptor"),
		METHOD_DESCRIPTOR("method descriptor"),
		OPERATOR("operator"),
		EOF("eof");

		final String name;

		TokenType(String name) {
			this.name = name;
		}
	}
}

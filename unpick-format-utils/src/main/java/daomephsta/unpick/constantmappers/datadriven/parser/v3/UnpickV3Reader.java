package daomephsta.unpick.constantmappers.datadriven.parser.v3;

import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupFormat;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupType;
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

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Performs syntax checking and basic semantic checking on .unpick v3 format text, and allows its structure to be
 * visited by instances of {@link UnpickV3Visitor}.
 */
public final class UnpickV3Reader implements AutoCloseable
{
	private static final int MAX_PARSE_DEPTH = 64;
	private static final EnumMap<BinaryExpression.Operator, Integer> PRECEDENCES = new EnumMap<>(BinaryExpression.Operator.class);
	static
	{
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
	private String nextToken;
	private ParseState nextTokenState;
	private String nextToken2;
	private ParseState nextToken2State;

	public UnpickV3Reader(Reader reader)
	{
		this.reader = new LineNumberReader(reader);
	}

	public void accept(UnpickV3Visitor visitor) throws IOException
	{
		line = reader.readLine();
		if (!"unpick v3".equals(line))
		{
			throw parseError("Missing version marker", 1, 0);
		}
		column = line.length();

		nextToken(); // newline

		while (true)
		{
			String token = nextToken();
			if (lastTokenType == TokenType.EOF)
			{
				break;
			}
			parseUnpickItem(visitor, token);
		}
	}

	private void parseUnpickItem(UnpickV3Visitor visitor, String token) throws IOException
	{
		if (lastTokenType != TokenType.IDENTIFIER)
		{
			throw expectedTokenError("unpick item", token);
		}

		switch (token)
		{
			case "target_field":
				visitor.visitTargetField(parseTargetField());
				break;
			case "target_method":
				visitor.visitTargetMethod(parseTargetMethod());
				break;
			case "scoped":
				GroupScope scope = parseGroupScope();
				token = nextToken("group type", TokenType.IDENTIFIER);
				switch (token)
				{
					case "const":
						visitor.visitGroupDefinition(parseGroupDefinition(scope, GroupType.CONST));
						break;
					case "flag":
						visitor.visitGroupDefinition(parseGroupDefinition(scope, GroupType.FLAG));
						break;
					default:
						throw expectedTokenError("group type", token);
				}
				break;
			case "const":
				visitor.visitGroupDefinition(parseGroupDefinition(GroupScope.Global.INSTANCE, GroupType.CONST));
				break;
			case "flag":
				visitor.visitGroupDefinition(parseGroupDefinition(GroupScope.Global.INSTANCE, GroupType.FLAG));
				break;
			default:
				throw expectedTokenError("unpick item", token);
		}
	}

	private GroupScope parseGroupScope() throws IOException
	{
		String token = nextToken("group scope type", TokenType.IDENTIFIER);
		switch (token)
		{
			case "package":
				return new GroupScope.Package(parseClassName("package name"));
			case "class":
				return new GroupScope.Class(parseClassName());
			case "method":
				String className = parseClassName();
				String methodName = parseMethodName();
				String methodDesc = nextToken(TokenType.METHOD_DESCRIPTOR);
				return new GroupScope.Method(className, methodName, methodDesc);
			default:
				throw expectedTokenError("group scope type", token);
		}
	}

	private GroupDefinition parseGroupDefinition(GroupScope scope, GroupType type) throws IOException
	{
		int typeLine = lastTokenLine;
		int typeColumn = lastTokenColumn;

		boolean strict = false;
		if ("strict".equals(peekToken()))
		{
			nextToken();
			strict = true;
		}

		DataType dataType = parseDataType();
		if (!isDataTypeValidInGroup(dataType))
		{
			throw parseError("Data type not allowed in group: " + dataType);
		}
		if (type == GroupType.FLAG && dataType != DataType.INT && dataType != DataType.LONG)
		{
			throw parseError("Data type not allowed for flag constants");
		}

		String name = peekTokenType() == TokenType.IDENTIFIER ? nextToken() : null;
		if (name == null && type != GroupType.CONST)
		{
			throw parseError("Non-const group type used for default group", typeLine, typeColumn);
		}

		List<GroupConstant> constants = new ArrayList<>();
		GroupFormat format = null;

		while (true)
		{
			String token = nextToken();
			if (lastTokenType == TokenType.EOF)
			{
				break;
			}
			if (lastTokenType != TokenType.NEWLINE)
			{
				throw expectedTokenError("'\\n'", token);
			}

			if (peekTokenType() != TokenType.INDENT)
			{
				break;
			}
			nextToken();

			token = nextToken();
			switch (lastTokenType)
			{
				case IDENTIFIER:
					if ("format".equals(token))
					{
						if (format != null)
						{
							throw parseError("Duplicate format declaration");
						}
						expectToken("=");
						format = parseGroupFormat();
						break;
					}
					// fallthrough
				case OPERATOR: case INTEGER: case DOUBLE: case CHAR: case STRING:
					int constantLine = lastTokenLine;
					int constantColumn = lastTokenColumn;
					GroupConstant constant = parseGroupConstant(token);
					if (!isMatchingConstantType(dataType, constant.key))
					{
						throw parseError("Constant type not valid for group data type", constantLine, constantColumn);
					}
					if (isDuplicateConstantKey(constants, constant))
					{
						throw parseError("Duplicate constant key", constantLine, constantColumn);
					}
					constants.add(constant);
					break;
				default:
					throw expectedTokenError("constant", token);
			}
		}

		return new GroupDefinition(scope, type, strict, dataType, name, constants, format);
	}

	private static boolean isDataTypeValidInGroup(DataType type)
	{
		return type == DataType.INT || type == DataType.LONG || type == DataType.FLOAT || type == DataType.DOUBLE || type == DataType.STRING || type == DataType.CLASS;
	}

	private static boolean isMatchingConstantType(DataType type, Literal.ConstantKey constantKey)
	{
		if (constantKey instanceof Literal.Long)
		{
			return type != DataType.STRING && type != DataType.CLASS;
		}
		else if (constantKey instanceof Literal.Double)
		{
			return type == DataType.FLOAT || type == DataType.DOUBLE;
		}
		else if (constantKey instanceof Literal.String)
		{
			return type == DataType.STRING;
		}
		else if (constantKey instanceof Literal.Class)
		{
			return type == DataType.CLASS;
		}
		else if (constantKey instanceof Literal.Null)
		{
			return type == DataType.STRING || type == DataType.CLASS;
		}
		else
		{
			throw new AssertionError("Unknown group constant type: " + constantKey.getClass().getName());
		}
	}

	private static boolean isDuplicateConstantKey(List<GroupConstant> constants, GroupConstant newConstant)
	{
		if (newConstant.key instanceof Literal.Long)
		{
			long newValue = ((Literal.Long) newConstant.key).value;
			for (GroupConstant constant : constants)
			{
				if (constant.key instanceof Literal.Long && ((Literal.Long) constant.key).value == newValue)
				{
					return true;
				}
				if (constant.key instanceof Literal.Double && ((Literal.Double) constant.key).value == newValue)
				{
					return true;
				}
			}
		}
		else if (newConstant.key instanceof Literal.Double)
		{
			double newValue = ((Literal.Double) newConstant.key).value;
			for (GroupConstant constant : constants)
			{
				if (constant.key instanceof Literal.Long && ((Literal.Long) constant.key).value == newValue)
				{
					return true;
				}
				if (constant.key instanceof Literal.Double && ((Literal.Double) constant.key).value == newValue)
				{
					return true;
				}
			}
		}
		else if (newConstant.key instanceof Literal.String)
		{
			String newValue = ((Literal.String) newConstant.key).value;
			for (GroupConstant constant : constants)
			{
				if (constant.key instanceof Literal.String && ((Literal.String) constant.key).value.equals(newValue))
				{
					return true;
				}
			}
		}
		else if (newConstant.key instanceof Literal.Class)
		{
			String newValue = ((Literal.Class) newConstant.key).descriptor;
			for (GroupConstant constant : constants)
			{
				if (constant.key instanceof Literal.Class && ((Literal.Class) constant.key).descriptor.equals(newValue))
				{
					return true;
				}
			}
		}
		else if (newConstant.key instanceof Literal.Null)
		{
			for (GroupConstant constant : constants)
			{
				if (constant.key instanceof Literal.Null)
				{
					return true;
				}
			}
		}

		return false;
	}

	private GroupFormat parseGroupFormat() throws IOException
	{
		String token = nextToken("group format", TokenType.IDENTIFIER);
		switch (token)
		{
			case "decimal":
				return GroupFormat.DECIMAL;
			case "hex":
				return GroupFormat.HEX;
			case "binary":
				return GroupFormat.BINARY;
			case "octal":
				return GroupFormat.OCTAL;
			case "char":
				return GroupFormat.CHAR;
			default:
				throw expectedTokenError("group format", token);
		}
	}

	private GroupConstant parseGroupConstant(String token) throws IOException
	{
		Literal.ConstantKey key = parseGroupConstantKey(token);
		expectToken("=");
		Expression value = parseExpression(0);
		return new GroupConstant(key, value);
	}

	private Literal.ConstantKey parseGroupConstantKey(String token) throws IOException
	{
		boolean negative = false;
		if ("-".equals(token))
		{
			negative = true;
			token = nextToken();
		}

		switch (lastTokenType)
		{
			case INTEGER:
				ParsedLong parsedLong = parseLong(token, negative);
				return new Literal.Long(parsedLong.value, parsedLong.radix);
			case DOUBLE:
				return new Literal.Double(parseDouble(token, negative));
			case CHAR:
				if (negative)
				{
					throw expectedTokenError("number", token);
				}
				return new Literal.Long(unquoteChar(token));
			case STRING:
				if (negative)
				{
					throw expectedTokenError("number", token);
				}
				return new Literal.String(unquoteString(token));
			case IDENTIFIER:
				switch (token)
				{
					case "NaN":
						if (negative)
						{
							throw expectedTokenError("number", token);
						}
						return new Literal.Double(Double.NaN);
					case "Infinity":
						return new Literal.Double(negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
					case "null":
						if (negative)
						{
							throw expectedTokenError("number", token);
						}
						return Literal.Null.INSTANCE;
					case "class":
						if (negative)
						{
							throw expectedTokenError("number", token);
						}
						String desc = nextToken(TokenType.CLASS_DESCRIPTOR);
						return new Literal.Class(desc);
					default:
						throw expectedTokenError(negative ? "number" : "constant", token);
				}
			default:
				throw expectedTokenError(negative ? "number" : "constant", token);
		}
	}

	private Expression parseExpression(int parseDepth) throws IOException
	{
		// Shunting yard algorithm for parsing with operator precedence: https://stackoverflow.com/a/47717/11071180
		Stack<Expression> operandStack = new Stack<>();
		Stack<BinaryExpression.Operator> operatorStack = new Stack<>();

		operandStack.push(parseUnaryExpression(parseDepth, false));

		parseLoop:
		while (true)
		{
			BinaryExpression.Operator operator;
			switch (peekToken())
			{
				case "|":
					operator = BinaryExpression.Operator.BIT_OR;
					break;
				case "^":
					operator = BinaryExpression.Operator.BIT_XOR;
					break;
				case "&":
					operator = BinaryExpression.Operator.BIT_AND;
					break;
				case "<<":
					operator = BinaryExpression.Operator.BIT_SHIFT_LEFT;
					break;
				case ">>":
					operator = BinaryExpression.Operator.BIT_SHIFT_RIGHT;
					break;
				case ">>>":
					operator = BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED;
					break;
				case "+":
					operator = BinaryExpression.Operator.ADD;
					break;
				case "-":
					operator = BinaryExpression.Operator.SUBTRACT;
					break;
				case "*":
					operator = BinaryExpression.Operator.MULTIPLY;
					break;
				case "/":
					operator = BinaryExpression.Operator.DIVIDE;
					break;
				case "%":
					operator = BinaryExpression.Operator.MODULO;
					break;
				default:
					break parseLoop;
			}
			nextToken(); // consume the operator

			int ourPrecedence = PRECEDENCES.get(operator);
			while (!operatorStack.isEmpty() && ourPrecedence <= PRECEDENCES.get(operatorStack.peek()))
			{
				BinaryExpression.Operator op = operatorStack.pop();
				Expression rhs = operandStack.pop();
				Expression lhs = operandStack.pop();
				operandStack.push(new BinaryExpression(lhs, rhs, op));
			}

			operatorStack.push(operator);
			operandStack.push(parseUnaryExpression(parseDepth, false));
		}

		Expression result = operandStack.pop();
		while (!operatorStack.isEmpty())
		{
			result = new BinaryExpression(operandStack.pop(), result, operatorStack.pop());
		}

		return result;
	}

	private Expression parseUnaryExpression(int parseDepth, boolean negative) throws IOException
	{
		if (parseDepth > MAX_PARSE_DEPTH)
		{
			throw parseError("max parse depth reached");
		}

		String token = nextToken();
		switch (token)
		{
			case "-":
				return new UnaryExpression(parseUnaryExpression(parseDepth + 1, true), UnaryExpression.Operator.NEGATE);
			case "~":
				return new UnaryExpression(parseUnaryExpression(parseDepth + 1, false), UnaryExpression.Operator.BIT_NOT);
			case "(":
				boolean parseAsCast = peekTokenType() == TokenType.IDENTIFIER && ")".equals(peekToken2());
				if (parseAsCast)
				{
					DataType castType = parseDataType();
					nextToken(); // close paren
					return new CastExpression(castType, parseUnaryExpression(parseDepth + 1, false));
				}
				else
				{
					Expression expression = parseExpression(parseDepth + 1);
					expectToken(")");
					return new ParenExpression(expression);
				}
		}

		switch (lastTokenType)
		{
			case IDENTIFIER:
				return parseFieldExpression(token);
			case INTEGER:
				ParsedInteger parsedInt = parseInt(token, negative);
				return new LiteralExpression(new Literal.Integer(negative ? -parsedInt.value : parsedInt.value, parsedInt.radix));
			case LONG:
				ParsedLong parsedLong = parseLong(token, negative);
				return new LiteralExpression(new Literal.Long(negative ? -parsedLong.value : parsedLong.value, parsedLong.radix));
			case FLOAT:
				float parsedFloat = parseFloat(token, negative);
				return new LiteralExpression(new Literal.Float(negative ? -parsedFloat : parsedFloat));
			case DOUBLE:
				double parsedDouble = parseDouble(token, negative);
				return new LiteralExpression(new Literal.Double(negative ? -parsedDouble : parsedDouble));
			case CHAR:
				return new LiteralExpression(new Literal.Character(unquoteChar(token)));
			case STRING:
				return new LiteralExpression(new Literal.String(unquoteString(token)));
			default:
				throw expectedTokenError("expression", token);
		}
	}

	private FieldExpression parseFieldExpression(String token) throws IOException
	{
		expectToken(".");
		String className = token + "." + parseClassName("field name");

		// the field name has been joined to the class name, split it off
		int dotIndex = className.lastIndexOf('.');
		String fieldName = className.substring(dotIndex + 1);
		className = className.substring(0, dotIndex);

		boolean isStatic = true;
		DataType fieldType = null;
		if (":".equals(peekToken()))
		{
			nextToken();
			if ("instance".equals(peekToken()))
			{
				nextToken();
				isStatic = false;
				if (":".equals(peekToken()))
				{
					nextToken();
					fieldType = parseDataType();
				}
			}
			else
			{
				fieldType = parseDataType();
			}
		}

		return new FieldExpression(className, fieldName, fieldType, isStatic);
	}

	private TargetField parseTargetField() throws IOException
	{
		String className = parseClassName();
		String fieldName = nextToken(TokenType.IDENTIFIER);
		String fieldDesc = nextToken(TokenType.CLASS_DESCRIPTOR);
		String groupName = nextToken(TokenType.IDENTIFIER);
		String token = nextToken();
		if (lastTokenType != TokenType.NEWLINE && lastTokenType != TokenType.EOF)
		{
			throw expectedTokenError("'\n'", token);
		}
		return new TargetField(className, fieldName, fieldDesc, groupName);
	}

	private TargetMethod parseTargetMethod() throws IOException
	{
		String className = parseClassName();
		String methodName = parseMethodName();
		String methodDesc = nextToken(TokenType.METHOD_DESCRIPTOR);

		Map<Integer, String> paramGroups = new HashMap<>();
		String returnGroup = null;

		while (true)
		{
			String token = nextToken();
			if (lastTokenType == TokenType.EOF)
			{
				break;
			}
			if (lastTokenType != TokenType.NEWLINE)
			{
				throw expectedTokenError("'\\n'", token);
			}

			if (peekTokenType() != TokenType.INDENT)
			{
				break;
			}
			nextToken();

			token = nextToken("target method item", TokenType.IDENTIFIER);
			switch (token)
			{
				case "param":
					int paramIndex = parseInt(nextToken(TokenType.INTEGER), false).value;
					if (paramGroups.containsKey(paramIndex))
					{
						throw parseError("Specified parameter " + paramIndex + " twice");
					}
					paramGroups.put(paramIndex, nextToken(TokenType.IDENTIFIER));
					break;
				case "return":
					if (returnGroup != null)
					{
						throw parseError("Specified return group twice");
					}
					returnGroup = nextToken(TokenType.IDENTIFIER);
					break;
				default:
					throw expectedTokenError("target method item", token);
			}
		}

		return new TargetMethod(className, methodName, methodDesc, paramGroups, returnGroup);
	}

	private DataType parseDataType() throws IOException
	{
		String token = nextToken("data type", TokenType.IDENTIFIER);
		switch (token)
		{
			case "byte":
				return DataType.BYTE;
			case "short":
				return DataType.SHORT;
			case "int":
				return DataType.INT;
			case "long":
				return DataType.LONG;
			case "float":
				return DataType.FLOAT;
			case "double":
				return DataType.DOUBLE;
			case "char":
				return DataType.CHAR;
			case "String":
				return DataType.STRING;
			case "Class":
				return DataType.CLASS;
			default:
				throw expectedTokenError("data type", token);
		}
	}

	private String parseClassName() throws IOException
	{
		return parseClassName("class name");
	}

	private String parseClassName(String expected) throws IOException
	{
		StringBuilder result = new StringBuilder(nextToken(expected, TokenType.IDENTIFIER));
		while (".".equals(peekToken()))
		{
			nextToken();
			result.append('.').append(nextToken(TokenType.IDENTIFIER));
		}
		return result.toString();
	}

	private String parseMethodName() throws IOException
	{
		String token = nextToken();
		if (lastTokenType == TokenType.IDENTIFIER)
		{
			return token;
		}
		if ("<".equals(token))
		{
			token = nextToken(TokenType.IDENTIFIER);
			if (!"init".equals(token) && !"clinit".equals(token))
			{
				throw expectedTokenError("identifier", token);
			}
			expectToken(">");
			return "<" + token + ">";
		}
		throw expectedTokenError("identifier", token);
	}

	private ParsedInteger parseInt(String string, boolean negative) throws UnpickSyntaxException
	{
		int radix;
		if (string.startsWith("0x") || string.startsWith("0X"))
		{
			radix = 16;
			string = string.substring(2);
		}
		else if (string.startsWith("0b") || string.startsWith("0B"))
		{
			radix = 2;
			string = string.substring(2);
		}
		else if (string.startsWith("0") && string.length() > 1)
		{
			radix = 8;
			string = string.substring(1);
		}
		else
		{
			radix = 10;
		}

		try
		{
			return new ParsedInteger(Integer.parseInt(negative ? "-" + string : string, radix), radix);
		}
		catch (NumberFormatException ignore)
		{
		}

		// try unsigned parsing in other radixes
		if (!negative && radix != 10)
		{
			try
			{
				return new ParsedInteger(Integer.parseUnsignedInt(string, radix), radix);
			}
			catch (NumberFormatException ignore)
			{
			}
		}

		throw parseError("Integer out of bounds");
	}

	private static final class ParsedInteger
	{
		final int value;
		final int radix;

		private ParsedInteger(int value, int radix)
		{
			this.value = value;
			this.radix = radix;
		}
	}

	private ParsedLong parseLong(String string, boolean negative) throws UnpickSyntaxException
	{
		if (string.endsWith("l") || string.endsWith("L"))
		{
			string = string.substring(0, string.length() - 1);
		}

		int radix;
		if (string.startsWith("0x") || string.startsWith("0X"))
		{
			radix = 16;
			string = string.substring(2);
		}
		else if (string.startsWith("0b") || string.startsWith("0B"))
		{
			radix = 2;
			string = string.substring(2);
		}
		else if (string.startsWith("0") && string.length() > 1)
		{
			radix = 8;
			string = string.substring(1);
		}
		else
		{
			radix = 10;
		}

		try
		{
			return new ParsedLong(Long.parseLong(negative ? "-" + string : string, radix), radix);
		}
		catch (NumberFormatException ignore)
		{
		}

		// try unsigned parsing in other radixes
		if (!negative && radix != 10)
		{
			try
			{
				return new ParsedLong(Long.parseUnsignedLong(string, radix), radix);
			}
			catch (NumberFormatException ignore)
			{
			}
		}

		throw parseError("Long out of bounds");
	}

	private static final class ParsedLong
	{
		final long value;
		final int radix;

		private ParsedLong(long value, int radix)
		{
			this.value = value;
			this.radix = radix;
		}
	}

	private float parseFloat(String string, boolean negative) throws UnpickSyntaxException
	{
		if (string.endsWith("f") || string.endsWith("F"))
		{
			string = string.substring(0, string.length() - 1);
		}
		try
		{
			float result = Float.parseFloat(string);
			return negative ? -result : result;
		}
		catch (NumberFormatException e)
		{
			throw parseError("Invalid float");
		}
	}

	private double parseDouble(String string, boolean negative) throws UnpickSyntaxException
	{
		try
		{
			double result = Double.parseDouble(string);
			return negative ? -result : result;
		}
		catch (NumberFormatException e)
		{
			throw parseError("Invalid double");
		}
	}

	private static char unquoteChar(String string)
	{
		return unquoteString(string).charAt(0);
	}

	private static String unquoteString(String string)
	{
		StringBuilder result = new StringBuilder(string.length() - 2);
		for (int i = 1; i < string.length() - 1; i++)
		{
			if (string.charAt(i) == '\\')
			{
				i++;
				switch (string.charAt(i))
				{
					case 'u':
						do
						{
							i++;
						} while (string.charAt(i) == 'u');
						result.append((char) Integer.parseInt(string.substring(i, i + 4), 16));
						i += 3;
						break;
					case 'b':
						result.append('\b');
						break;
					case 't':
						result.append('\t');
						break;
					case 'n':
						result.append('\n');
						break;
					case 'f':
						result.append('\f');
						break;
					case 'r':
						result.append('\r');
						break;
					case '"':
						result.append('"');
						break;
					case '\'':
						result.append('\'');
						break;
					case '\\':
						result.append('\\');
						break;
					case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7':
						char c;
						int count = 0;
						int maxCount = string.charAt(i) <= '3' ? 3 : 2;
						while (count < maxCount && (c = string.charAt(i + count)) >= '0' && c <= '7')
						{
							count++;
						}
						result.append((char) Integer.parseInt(string.substring(i, i + count), 8));
						i += count - 1;
						break;
					default:
						throw new AssertionError("Unexpected escape sequence in string");
				}
			}
			else
			{
				result.append(string.charAt(i));
			}
		}
		return result.toString();
	}

	// region Tokenizer

	private TokenType peekTokenType() throws IOException
	{
		ParseState state = new ParseState(this);
		nextToken = nextToken();
		nextTokenState = new ParseState(this);
		state.restore(this);
		return nextTokenState.lastTokenType;
	}

	private String peekToken() throws IOException
	{
		ParseState state = new ParseState(this);
		nextToken = nextToken();
		nextTokenState = new ParseState(this);
		state.restore(this);
		return nextToken;
	}

	private String peekToken2() throws IOException
	{
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

	private void expectToken(String expected) throws IOException
	{
		String token = nextToken();
		if (!expected.equals(token))
		{
			throw expectedTokenError(UnpickV3Writer.quoteString(expected, '\''), token);
		}
	}

	private String nextToken() throws IOException
	{
		return nextTokenInner(null);
	}

	private String nextToken(TokenType type) throws IOException
	{
		return nextToken(type.name, type);
	}

	private String nextToken(String expected, TokenType type) throws IOException
	{
		String token = nextTokenInner(type);
		if (lastTokenType != type)
		{
			throw expectedTokenError(expected, token);
		}
		return token;
	}

	private String nextTokenInner(@Nullable TokenType typeHint) throws IOException
	{
		if (nextTokenState != null)
		{
			String tok = nextToken;
			nextToken = nextToken2;
			nextToken2 = null;
			nextTokenState.restore(this);
			nextTokenState = nextToken2State;
			nextToken2State = null;
			return tok;
		}

		if (lastTokenType == TokenType.EOF)
		{
			return null;
		}

		// newline token (skipping comment and whitespace)
		while (column < line.length() && Character.isWhitespace(line.charAt(column)))
		{
			column++;
		}
		if (column < line.length() && line.charAt(column) == '#')
		{
			column = line.length();
		}
		if (column == line.length() && lastTokenType != TokenType.NEWLINE)
		{
			lastTokenColumn = column;
			lastTokenLine = reader.getLineNumber();
			lastTokenType = TokenType.NEWLINE;
			return "\n";
		}

		// skip whitespace and comments, handle indent token
		boolean seenIndent = false;
		while (true)
		{
			if (column == line.length() || line.charAt(column) == '#')
			{
				seenIndent = false;
				line = reader.readLine();
				column = 0;
				if (line == null)
				{
					lastTokenColumn = column;
					lastTokenLine = reader.getLineNumber();
					lastTokenType = TokenType.EOF;
					return null;
				}
			}
			else if (Character.isWhitespace(line.charAt(column)))
			{
				seenIndent = column == 0;
				do
				{
					column++;
				} while (column < line.length() && Character.isWhitespace(line.charAt(column)));
			}
			else
			{
				break;
			}
		}
		if (seenIndent)
		{
			lastTokenColumn = 0;
			lastTokenLine = reader.getLineNumber();
			lastTokenType = TokenType.INDENT;
			return line.substring(0, column);
		}

		lastTokenColumn = column;
		lastTokenLine = reader.getLineNumber();

		if (typeHint == TokenType.CLASS_DESCRIPTOR)
		{
			if (skipFieldDescriptor(true))
			{
				return line.substring(lastTokenColumn, column);
			}
		}

		if (typeHint == TokenType.METHOD_DESCRIPTOR)
		{
			if (skipMethodDescriptor())
			{
				return line.substring(lastTokenColumn, column);
			}
		}

		if (skipNumber())
		{
			if (column < line.length() && isIdentifierChar(line.charAt(column)))
			{
				throw parseErrorInToken("Unexpected character in number: " + line.charAt(column));
			}
			return line.substring(lastTokenColumn, column);
		}

		if (skipIdentifier())
		{
			return line.substring(lastTokenColumn, column);
		}

		if (skipString('\'', true))
		{
			lastTokenType = TokenType.CHAR;
			return line.substring(lastTokenColumn, column);
		}

		if (skipString('"', false))
		{
			lastTokenType = TokenType.STRING;
			return line.substring(lastTokenColumn, column);
		}

		char c = line.charAt(column);
		column++;
		if (c == '<')
		{
			if (column < line.length() && line.charAt(column) == '<')
			{
				column++;
			}
		}
		else if (c == '>')
		{
			if (column < line.length() && line.charAt(column) == '>')
			{
				column++;
				if (column < line.length() && line.charAt(column) == '>')
				{
					column++;
				}
			}
		}

		lastTokenType = TokenType.OPERATOR;
		return line.substring(lastTokenColumn, column);
	}

	private boolean skipFieldDescriptor(boolean startOfToken) throws UnpickSyntaxException
	{
		// array descriptors
		while (column < line.length() && line.charAt(column) == '[')
		{
			startOfToken = false;
			column++;
		}

		// first character of main part of descriptor
		if (column == line.length() || isTokenEnd(line.charAt(column)))
		{
			throw parseErrorInToken("Unexpected end to descriptor");
		}
		switch (line.charAt(column))
		{
			// primitive types
			case 'B': case 'C': case 'D': case 'F': case 'I': case 'J': case 'S': case 'Z':
				column++;
				break;
			// class types
			case 'L':
				column++;

				// class name
				char c;
				while (column < line.length() && (c = line.charAt(column)) != ';' && !isTokenEnd(c))
				{
					if (c == '.' || c == '[')
					{
						throw parseErrorInToken("Illegal character in descriptor: " + c);
					}
					column++;
				}

				// semicolon
				if (column == line.length() || isTokenEnd(line.charAt(column)))
				{
					throw parseErrorInToken("Unexpected end of descriptor");
				}
				column++;
				break;
			default:
				if (!startOfToken)
				{
					throw parseErrorInToken("Illegal character in descriptor: " + line.charAt(column));
				}
				return false;
		}

		lastTokenType = TokenType.CLASS_DESCRIPTOR;
		return true;
	}

	private boolean skipMethodDescriptor() throws UnpickSyntaxException
	{
		if (line.charAt(column) != '(')
		{
			return false;
		}
		column++;

		// parameter types
		while (column < line.length() && line.charAt(column) != ')' && !isTokenEnd(line.charAt(column)))
		{
			skipFieldDescriptor(false);
		}
		if (column == line.length() || isTokenEnd(line.charAt(column)))
		{
			throw parseErrorInToken("Unexpected end of descriptor");
		}
		column++;

		// return type
		if (column == line.length() || isTokenEnd(line.charAt(column)))
		{
			throw parseErrorInToken("Unexpected end of descriptor");
		}
		if (line.charAt(column) == 'V')
		{
			column++;
		}
		else
		{
			skipFieldDescriptor(false);
		}

		lastTokenType = TokenType.METHOD_DESCRIPTOR;
		return true;
	}

	private boolean skipNumber() throws UnpickSyntaxException
	{
		if (line.charAt(column) < '0' || line.charAt(column) > '9')
		{
			return false;
		}

		// hex numbers
		if (line.startsWith("0x", column) || line.startsWith("0X", column))
		{
			column += 2;
			char c;
			boolean seenDigit = false;
			while (column < line.length() && ((c = line.charAt(column)) >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F'))
			{
				seenDigit = true;
				column++;
			}
			if (!seenDigit)
			{
				throw parseErrorInToken("Unexpected end of integer");
			}
			detectIntegerType();
			return true;
		}

		// binary numbers
		if (line.startsWith("0b", column) || line.startsWith("0B", column))
		{
			column += 2;
			char c;
			boolean seenDigit = false;
			while (column < line.length() && ((c = line.charAt(column)) == '0' || c == '1'))
			{
				seenDigit = true;
				column++;
			}
			if (!seenDigit)
			{
				throw parseErrorInToken("Unexpected end of integer");
			}
			detectIntegerType();
			return true;
		}

		// lookahead a decimal number
		int endOfInteger = column;
		char c;
		do
		{
			endOfInteger++;
		} while (endOfInteger < line.length() && (c = line.charAt(endOfInteger)) >= '0' && c <= '9');

		// floats and doubles
		if (endOfInteger < line.length() && line.charAt(endOfInteger) == '.')
		{
			column = endOfInteger + 1;

			// fractional part
			boolean seenFracDigit = false;
			while (column < line.length() && (c = line.charAt(column)) >= '0' && c <= '9')
			{
				seenFracDigit = true;
				column++;
			}
			if (!seenFracDigit)
			{
				throw parseErrorInToken("Unexpected end of float");
			}

			// exponent
			if (column < line.length() && ((c = line.charAt(column)) == 'e' || c == 'E'))
			{
				column++;
				if (column < line.length() && (c = line.charAt(column)) >= '+' && c <= '-')
				{
					column++;
				}

				boolean seenExponentDigit = false;
				while (column < line.length() && ((c = line.charAt(column)) >= '0' && c <= '9'))
				{
					seenExponentDigit = true;
					column++;
				}
				if (!seenExponentDigit)
				{
					throw parseErrorInToken("Unexpected end of float");
				}
			}

			boolean isFloat = column < line.length() && ((c = line.charAt(column)) == 'f' || c == 'F');
			if (isFloat)
			{
				column++;
			}
			lastTokenType = isFloat ? TokenType.FLOAT : TokenType.DOUBLE;
			return true;
		}

		// octal numbers (we'll count 0 itself as an octal)
		if (line.charAt(column) == '0')
		{
			column++;
			while (column < line.length() && (c = line.charAt(column)) >= '0' && c <= '7')
			{
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

	private void detectIntegerType()
	{
		char c;
		boolean isLong = column < line.length() && ((c = line.charAt(column)) == 'l' || c == 'L');
		if (isLong)
		{
			column++;
		}
		lastTokenType = isLong ? TokenType.LONG : TokenType.INTEGER;
	}

	private boolean skipIdentifier()
	{
		if (!isIdentifierChar(line.charAt(column)))
		{
			return false;
		}

		do
		{
			column++;
		} while (column < line.length() && isIdentifierChar(line.charAt(column)));

		lastTokenType = TokenType.IDENTIFIER;
		return true;
	}

	private boolean skipString(char quoteChar, boolean singleChar) throws UnpickSyntaxException
	{
		if (line.charAt(column) != quoteChar)
		{
			return false;
		}
		column++;

		boolean seenChar = false;
		while (column < line.length() && line.charAt(column) != quoteChar)
		{
			if (singleChar && seenChar)
			{
				throw parseErrorInToken("Multiple characters in char literal");
			}
			seenChar = true;

			if (line.charAt(column) == '\\')
			{
				column++;
				if (column == line.length())
				{
					throw parseErrorInToken("Unexpected end of string");
				}
				char c = line.charAt(column);
				switch (c)
				{
					case 'u':
						do
						{
							column++;
						} while (column < line.length() && line.charAt(column) == 'u');
						for (int i = 0; i < 4; i++)
						{
							if (column == line.length())
							{
								throw parseErrorInToken("Unexpected end of string");
							}
							c = line.charAt(column);
							if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F'))
							{
								throw parseErrorInToken("Illegal character in unicode escape sequence");
							}
							column++;
						}
						break;
					case 'b': case 't': case 'n': case 'f': case 'r': case '"': case '\'': case '\\':
						column++;
						break;
					case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7':
						column++;
						int maxOctalDigits = c <= '3' ? 3 : 2;
						for (int i = 1; i < maxOctalDigits && column < line.length() && (c = line.charAt(column)) >= '0' && c <= '7'; i++)
						{
							column++;
						}
						break;
					default:
						throw parseErrorInToken("Illegal escape sequence \\" + c);
				}
			}
			else
			{
				column++;
			}
		}

		if (column == line.length())
		{
			throw parseErrorInToken("Unexpected end of string");
		}

		if (singleChar && !seenChar)
		{
			throw parseErrorInToken("No character in char literal");
		}

		column++;
		return true;
	}

	private static boolean isTokenEnd(char c)
	{
		return Character.isWhitespace(c) || c == '#';
	}

	private static boolean isIdentifierChar(char c)
	{
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '$';
	}

	// endregion

	private UnpickSyntaxException expectedTokenError(String expected, String token)
	{
		if (lastTokenType == TokenType.EOF)
		{
			return parseError("Expected " + expected + " before eof token");
		}
		else
		{
			return parseError("Expected " + expected + " before " + UnpickV3Writer.quoteString(token, '\'') + " token");
		}
	}

	private UnpickSyntaxException parseError(String message)
	{
		return parseError(message, lastTokenLine, lastTokenColumn);
	}

	private UnpickSyntaxException parseErrorInToken(String message)
	{
		return parseError(message, reader.getLineNumber(), column);
	}

	private UnpickSyntaxException parseError(String message, int lineNumber, int column)
	{
		return new UnpickSyntaxException(lineNumber, column + 1, message);
	}

	@Override
	public void close() throws IOException
	{
		reader.close();
	}

	private static class ParseState
	{
		private final int lastTokenLine;
		private final int lastTokenColumn;
		private final TokenType lastTokenType;

		ParseState(UnpickV3Reader reader)
		{
			this.lastTokenLine = reader.lastTokenLine;
			this.lastTokenColumn = reader.lastTokenColumn;
			this.lastTokenType = reader.lastTokenType;
		}

		void restore(UnpickV3Reader reader)
		{
			reader.lastTokenLine = lastTokenLine;
			reader.lastTokenColumn = lastTokenColumn;
			reader.lastTokenType = lastTokenType;
		}
	}

	private enum TokenType
	{
		IDENTIFIER("identifier"),
		DOUBLE("double"),
		FLOAT("float"),
		INTEGER("integer"),
		LONG("long"),
		CHAR("char"),
		STRING("string"),
		INDENT("indent"),
		NEWLINE("newline"),
		CLASS_DESCRIPTOR("classF descriptor"),
		METHOD_DESCRIPTOR("method descriptor"),
		OPERATOR("operator"),
		EOF("eof");

		final String name;

		TokenType(String name)
		{
			this.name = name;
		}
	}
}

package daomephsta.unpick.impl.constantmappers.datadriven.parser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.impl.constantmappers.datadriven.data.Data;
import daomephsta.unpick.impl.constantmappers.datadriven.parser.v2.V2Parser;

public final class V1Parser {
	private V1Parser() {
	}

	private static final Pattern WHITESPACE_SPLITTER = Pattern.compile("\\s");

	public static void parse(Reader mappingSource, IConstantResolver constantResolver, Data data) throws IOException {
		try (LineNumberReader reader = new LineNumberReader(mappingSource)) {
			reader.readLine(); // skip version

			String line = "";
			while ((line = reader.readLine()) != null) {
				line = stripComment(line).trim();
				if (line.isEmpty()) {
					continue;
				}

				String[] tokens = tokenize(line);
				if (tokens.length == 0) {
					continue;
				}

				switch (tokens[0]) {
					case "constant" ->
							data.visitGroupDefinition(parseGroupDefinition(false, constantResolver, tokens, reader.getLineNumber()));
					case "flag" ->
							data.visitGroupDefinition(parseGroupDefinition(true, constantResolver, tokens, reader.getLineNumber()));
					case "unpick" ->
							data.visitTargetMethod(parseTargetMethodDefinition(tokens, reader.getLineNumber()));
					default ->
							throw new UnpickSyntaxException(reader.getLineNumber(), "Unknown start token " + tokens[0]);
				}
			}
		}
	}

	private static String stripComment(String in) {
		int c = in.indexOf('#');
		return c == -1 ? in : in.substring(0, c);
	}

	private static String[] tokenize(String in) {
		List<String> result = new ArrayList<>();

		for (String s : WHITESPACE_SPLITTER.split(in)) {
			if (!s.isEmpty()) {
				result.add(s);
			}
		}

		return result.toArray(new String[0]);
	}

	private static GroupDefinition parseGroupDefinition(boolean flags, IConstantResolver constantResolver, String[] tokens, int lineNumber) {
		if (tokens.length != 4 && tokens.length != 6) {
			throw new UnpickSyntaxException(lineNumber, "Unexpected token count. Expected 4 or 6. Found " + tokens.length);
		}

		String group = tokens[1];
		String owner = tokens[2];
		String name = tokens[3];
		DataType dataType;

		boolean explicitType = tokens.length > 4;
		if (explicitType) {
			dataType = V2Parser.parseType(tokens[5], lineNumber);
		} else {
			IConstantResolver.ResolvedConstant constant = constantResolver.resolveConstant(owner, name);
			if (constant == null) {
				throw new UnpickSyntaxException(lineNumber, "Cannot resolve constant " + owner + "." + name);
			}
			dataType = V2Parser.parseType(constant.type().getDescriptor(), lineNumber);
		}

		DataType groupDataType = V2Parser.widenGroupType(dataType);

		GroupDefinition.Builder groupDefinition = GroupDefinition.Builder.named(groupDataType, group)
				.constant(
						new FieldExpression(
								owner.replace('/', '.'),
								name,
								explicitType ? dataType : null,
								true
						)
				);
		if (flags) {
			groupDefinition.flags();
		}
		return groupDefinition.build();
	}

	private static TargetMethod parseTargetMethodDefinition(String[] tokens, int lineNumber) {
		if (tokens.length < 4 || tokens.length % 2 != 0) {
			throw new UnpickSyntaxException(lineNumber, "Unexpected token count. Expected an even number greater than or equal to 4. Found " + tokens.length);
		}

		String owner = tokens[1];
		String name = tokens[2];
		String desc = tokens[3];
		Map<Integer, String> parameterGroups = new HashMap<>();

		for (int p = 5; p < tokens.length; p += 2) {
			try {
				int parameterIndex = Integer.parseInt(tokens[p - 1]);
				if (parameterGroups.put(parameterIndex, tokens[p]) != null) {
					throw new UnpickSyntaxException(lineNumber, "Duplicate parameter index " + parameterIndex);
				}
			} catch (NumberFormatException e) {
				throw new UnpickSyntaxException(lineNumber, "Could not parse " + tokens[p - 1] + " as integer", e);
			}
		}

		return new TargetMethod(
			owner.replace('/', '.'),
			name,
			desc,
			parameterGroups,
			null
		);
	}
}

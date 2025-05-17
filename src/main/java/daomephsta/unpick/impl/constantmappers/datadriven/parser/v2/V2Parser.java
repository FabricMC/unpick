package daomephsta.unpick.impl.constantmappers.datadriven.parser.v2;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.logging.Logger;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.TargetMethodDefinitionVisitor;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.impl.constantmappers.datadriven.data.Data;

public final class V2Parser implements Visitor {
	private final Logger logger;
	private final IConstantResolver constantResolver;
	private final Data data;
	private int lineNumber;

	private V2Parser(Logger logger, IConstantResolver constantResolver, Data data) {
		this.logger = logger;
		this.constantResolver = constantResolver;
		this.data = data;
	}

	public static void parse(Logger logger, Reader mappingSource, IConstantResolver constantResolver, Data data) throws IOException {
		try (UnpickV2Reader unpickDefinitions = new UnpickV2Reader(mappingSource)) {
			unpickDefinitions.accept(new V2Parser(logger, constantResolver, data));
		}
	}

	public static DataType parseType(String descriptor, int lineNumber) {
		return switch (descriptor) {
			case "B" -> DataType.BYTE;
			case "C" -> DataType.CHAR;
			case "D" -> DataType.DOUBLE;
			case "F" -> DataType.FLOAT;
			case "I" -> DataType.INT;
			case "J" -> DataType.LONG;
			case "S" -> DataType.SHORT;
			case "Ljava/lang/String;" -> DataType.STRING;
			default -> throw new UnpickSyntaxException(lineNumber, "Invalid constant type " + descriptor);
		};
	}

	public static DataType widenGroupType(DataType groupType) {
		return switch (groupType) {
			case BYTE, SHORT, CHAR -> DataType.INT;
			default -> groupType;
		};
	}

	@Override
	public void visitLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Override
	public void visitSimpleConstantDefinition(String groupId, String owner, String name, String value, String descriptor) {
		visitConstantDefinition(false, groupId, owner, name, descriptor);
	}

	@Override
	public void visitFlagConstantDefinition(String groupId, String owner, String name, String value, String descriptor) {
		visitConstantDefinition(true, groupId, owner, name, descriptor);
	}

	private void visitConstantDefinition(boolean flags, String groupId, String owner, String name, @Nullable String descriptor) {
		DataType dataType;
		if (descriptor != null) {
			dataType = parseType(descriptor, lineNumber);
		} else {
			IConstantResolver.ResolvedConstant constant = constantResolver.resolveConstant(owner, name);
			if (constant == null) {
				logger.warning(() -> "Constant '" + owner + "." + name + "' not found");
				return;
			}
			dataType = parseType(constant.type().getDescriptor(), lineNumber);
		}

		DataType groupDataType = widenGroupType(dataType);

		GroupDefinition.Builder groupDefinition = GroupDefinition.Builder.named(groupDataType, groupId)
				.constant(
						new FieldExpression(
								owner.replace('/', '.'),
								name,
								null,
								true
						)
				);
		if (flags) {
			groupDefinition.flags();
		}
		data.visitGroupDefinition(groupDefinition.build());
	}

	@Override
	public TargetMethodDefinitionVisitor visitTargetMethodDefinition(String owner, String name, String descriptor) {
		return new TargetMethodParser(data, owner.replace('/', '.'), name, descriptor, () -> lineNumber);
	}

	private static class TargetMethodParser implements TargetMethodDefinitionVisitor {
		private final Data data;
		private final String owner;
		private final String name;
		private final String descriptor;
		private final Map<Integer, String> parameterGroups = new HashMap<>();
		@Nullable
		private String returnGroup;
		private final IntSupplier lineNumber;

		TargetMethodParser(Data data, String owner, String name, String descriptor, IntSupplier lineNumber) {
			this.data = data;
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.lineNumber = lineNumber;
		}

		@Override
		public void visitParameterGroupDefinition(int parameterIndex, String group) {
			if (parameterGroups.put(parameterIndex, group) != null) {
				throw new UnpickSyntaxException(lineNumber.getAsInt(), "Duplicate parameter index " + parameterIndex);
			}
		}

		@Override
		public void visitReturnGroupDefinition(String group) {
			if (returnGroup != null) {
				throw new UnpickSyntaxException(lineNumber.getAsInt(), "Duplicate return group " + returnGroup);
			}

			returnGroup = group;
		}

		@Override
		public void endVisit() {
			data.visitTargetMethod(new TargetMethod(
					owner,
					name,
					descriptor,
					parameterGroups,
					returnGroup
			));
		}
	}
}

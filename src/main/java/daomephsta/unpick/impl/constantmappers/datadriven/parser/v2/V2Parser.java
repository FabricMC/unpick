package daomephsta.unpick.impl.constantmappers.datadriven.parser.v2;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupType;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantGrouper;
import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.TargetMethodDefinitionVisitor;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.Visitor;

public final class V2Parser implements Visitor
{
	private final IConstantResolver constantResolver;
	private final DataDrivenConstantGrouper.Data data;
	private int lineNumber;

	private V2Parser(IConstantResolver constantResolver, DataDrivenConstantGrouper.Data data)
	{
		this.constantResolver = constantResolver;
		this.data = data;
	}

	public static void parse(Reader mappingSource, IConstantResolver constantResolver, DataDrivenConstantGrouper.Data data) throws IOException
	{
		try (UnpickV2Reader unpickDefinitions = new UnpickV2Reader(mappingSource))
		{
			unpickDefinitions.accept(new V2Parser(constantResolver, data));
		}
	}

	public static DataType parseType(String descriptor, int lineNumber)
	{
		switch (descriptor)
		{
			case "B":
				return DataType.BYTE;
			case "C":
				return DataType.CHAR;
			case "D":
				return DataType.DOUBLE;
			case "F":
				return DataType.FLOAT;
			case "I":
				return DataType.INT;
			case "J":
				return DataType.LONG;
			case "S":
				return DataType.SHORT;
			case "Ljava/lang/String;":
				return DataType.STRING;
			default:
				throw new UnpickSyntaxException(lineNumber, "Invalid constant type " + descriptor);
		}
	}

	public static DataType widenGroupType(DataType groupType)
	{
		switch (groupType)
		{
			case BYTE:
			case SHORT:
			case CHAR:
				return DataType.INT;
			default:
				return groupType;
		}
	}

	public static Object parseConstantKeyValue(DataType dataType, String constant, int lineNumber)
	{
		try
		{
			switch (dataType)
			{
				case DOUBLE:
				case FLOAT:
					return Double.parseDouble(constant);
				case BYTE:
				case CHAR:
				case INT:
				case LONG:
				case SHORT:
					return Long.parseLong(constant);
				case STRING:
					return constant;
				default:
					throw new UnpickSyntaxException(lineNumber, "Invalid constant type " + dataType);
			}
		}
		catch (NumberFormatException e)
		{
			throw new UnpickSyntaxException(lineNumber, "Invalid constant " + constant);
		}
	}

	public static Literal.ConstantKey objectToConstantKey(Object object, int lineNumber)
	{
		if (object instanceof Float)
		{
			return new Literal.Double(((Float) object).doubleValue());
		}
		else if (object instanceof Double)
		{
			return new Literal.Double((Double) object);
		}
		else if (object instanceof Number)
		{
			return new Literal.Long(((Number) object).longValue());
		}
		else if (object instanceof String)
		{
			return new Literal.String((String) object);
		}
		else
		{
			throw new UnpickSyntaxException(lineNumber, "Invalid constant type " + object.getClass().getName());
		}
	}
	
	@Override
	public void visitLineNumber(int lineNumber)
	{
		this.lineNumber = lineNumber;
	}
	
	@Override
	public void visitSimpleConstantDefinition(String groupId, String owner, String name, String value, String descriptor)
	{
		visitConstantDefinition(GroupType.CONST, groupId, owner, name, value, descriptor);
	}

	@Override
	public void visitFlagConstantDefinition(String groupId, String owner, String name, String value, String descriptor)
	{
		visitConstantDefinition(GroupType.FLAG, groupId, owner, name, value, descriptor);
	}

	private void visitConstantDefinition(GroupType groupType, String groupId, String owner, String name, @Nullable String value, @Nullable String descriptor)
	{
		DataType dataType;
		Object valueObj;

		if (value == null || descriptor == null)
		{
			IConstantResolver.ResolvedConstant constant = constantResolver.resolveConstant(owner, name);
			if (constant == null)
			{
				throw new UnpickSyntaxException(lineNumber, "Constant '" + owner + "." + name + "' not found");
			}
			dataType = parseType(constant.getType().getDescriptor(), lineNumber);
			valueObj = constant.getValue();
		}
		else
		{
			dataType = parseType(descriptor, lineNumber);
			valueObj = parseConstantKeyValue(dataType, value, lineNumber);
		}

		DataType groupDataType = widenGroupType(dataType);

		data.visitGroupDefinition(
			GroupDefinition.Builder.named(groupDataType, groupId)
					.type(groupType)
					.constant(
							new GroupConstant(
									objectToConstantKey(valueObj, lineNumber),
									new FieldExpression(
											owner.replace('/', '.'),
											name,
											dataType == groupDataType ? null : dataType,
											true
									)
							)
					)
					.build()
		);
	}

	@Override
	public TargetMethodDefinitionVisitor visitTargetMethodDefinition(String owner, String name, String descriptor)
	{
		return new TargetMethodParser(data, owner.replace('/', '.'), name, descriptor, () -> lineNumber);
	}
	
	private static class TargetMethodParser implements TargetMethodDefinitionVisitor
	{
		private final DataDrivenConstantGrouper.Data data;
		private final String owner;
		private final String name;
		private final String descriptor;
		private final Map<Integer, String> parameterGroups = new HashMap<>();
		@Nullable
		private String returnGroup;
		private final IntSupplier lineNumber;
		
		public TargetMethodParser(DataDrivenConstantGrouper.Data data, String owner, String name, String descriptor, IntSupplier lineNumber)
		{
			this.data = data;
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.lineNumber = lineNumber;
		}

		@Override
		public void visitParameterGroupDefinition(int parameterIndex, String group)
		{
			if (parameterGroups.put(parameterIndex, group) != null)
			{
				throw new UnpickSyntaxException(lineNumber.getAsInt(), "Duplicate parameter index " + parameterIndex);
			}
		}

		@Override
		public void visitReturnGroupDefinition(String group)
		{
			if (returnGroup != null)
			{
				throw new UnpickSyntaxException(lineNumber.getAsInt(), "Duplicate return group " + returnGroup);
			}

			returnGroup = group;
		}
		
		@Override
		public void endVisit()
		{
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

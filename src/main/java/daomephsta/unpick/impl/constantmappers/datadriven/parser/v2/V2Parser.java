package daomephsta.unpick.impl.constantmappers.datadriven.parser.v2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.IntSupplier;

import org.objectweb.asm.Type;

import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.TargetMethodDefinitionVisitor;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.Visitor;
import daomephsta.unpick.impl.representations.*;
import daomephsta.unpick.impl.representations.TargetMethods.DuplicateMappingException;
import daomephsta.unpick.impl.representations.TargetMethods.TargetMethodBuilder;

public class V2Parser implements Visitor
{
	private final Map<String, AbstractConstantGroup<?>> constantGroups;
	private final TargetMethods.Builder targetMethodsBuilder;
	private int lineNumber;

	public V2Parser(Map<String, AbstractConstantGroup<?>> constantGroups, TargetMethods.Builder targetMethodsBuilder)
	{
		this.constantGroups = constantGroups;
		this.targetMethodsBuilder = targetMethodsBuilder;
	}

	public static void parse(InputStream mappingSource, Map<String, AbstractConstantGroup<?>> constantGroups, TargetMethods.Builder targetMethodsBuilder) throws IOException
	{
		try (UnpickV2Reader unpickDefinitions = new UnpickV2Reader(mappingSource))
		{
			unpickDefinitions.accept(new V2Parser(constantGroups, targetMethodsBuilder));
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
		ReplacementInstructionGenerator group = constantGroups.computeIfAbsent(groupId, k -> new SimpleConstantGroup(k));
		SimpleConstantDefinition constant = createSimpleConstantDefinition(owner, name, value, descriptor);
		if (group instanceof SimpleConstantGroup)
			((SimpleConstantGroup) group).add(constant);
		else
			throw new UnpickSyntaxException(lineNumber, String.format("Cannot add simple constant to %s '%s'", group.getClass().getSimpleName(), groupId));
	}

	private SimpleConstantDefinition createSimpleConstantDefinition(String owner, String name, String value, String descriptor)
	{
		if (value != null && descriptor != null)
		{
			try 
			{
				return new SimpleConstantDefinition(owner, name, Type.getType(descriptor), value);
			}
			catch (IllegalArgumentException e)
			{
				throw new UnpickSyntaxException(lineNumber, "Unable to parse descriptor " + descriptor);
			}
		}
		return new SimpleConstantDefinition(owner, name);
	}

	@Override
	public void visitFlagConstantDefinition(String groupId, String owner, String name, String value, String descriptor)
	{
		ReplacementInstructionGenerator group = constantGroups.computeIfAbsent(groupId, k -> new FlagConstantGroup(k));
		FlagDefinition constant = createFlagDefinition(owner, name, value, descriptor);
		if (group instanceof FlagConstantGroup)
			((FlagConstantGroup) group).add(constant);
		else
			throw new UnpickSyntaxException(lineNumber, String.format("Cannot add flag constant to %s '%s'", group.getClass().getSimpleName(), groupId));
	}

	private FlagDefinition createFlagDefinition(String owner, String name, String value, String descriptor)
	{
		if (value != null && descriptor != null)
		{
			try 
			{
				return new FlagDefinition(owner, name, Type.getType(descriptor), value);
			}
			catch (IllegalArgumentException e)
			{
				throw new UnpickSyntaxException(lineNumber, "Unable to parse descriptor " + descriptor);
			}
		}
		return new FlagDefinition(owner, name);
	}

	@Override
	public TargetMethodDefinitionVisitor visitTargetMethodDefinition(String owner, String name, String descriptor)
	{
		return new TargetMethodParser(targetMethodsBuilder.targetMethod(owner, name, Type.getType(descriptor)), () -> lineNumber);
	}
	
	private static class TargetMethodParser implements TargetMethodDefinitionVisitor
	{
		private final TargetMethods.TargetMethodBuilder targetMethodBuilder;
		private final IntSupplier lineNumber;
		
		public TargetMethodParser(TargetMethodBuilder targetMethodBuilder, IntSupplier lineNumber)
		{
			this.targetMethodBuilder = targetMethodBuilder;
			this.lineNumber = lineNumber;
		}

		@Override
		public void visitParameterGroupDefinition(int parameterIndex, String group)
		{
			try 
			{
				targetMethodBuilder.parameterGroup(parameterIndex, group);
			}
			catch (DuplicateMappingException e)
			{
				throw new UnpickSyntaxException(lineNumber.getAsInt(), e.getMessage());
			}
		}

		@Override
		public void visitReturnGroupDefinition(String group)
		{
			try
			{
				targetMethodBuilder.returnGroup(group);
			}
			catch (DuplicateMappingException e)
			{
				throw new UnpickSyntaxException(lineNumber.getAsInt(), e.getMessage());
			}
		}
		
		@Override
		public void endVisit()
		{
			targetMethodBuilder.add();
		}
	}
}

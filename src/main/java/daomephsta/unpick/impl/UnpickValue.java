package daomephsta.unpick.impl;

import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.HashSet;
import java.util.Set;

public class UnpickValue implements IReplacementGenerator.IDataflowValue
{
	private final Type dataType;
	private Set<Integer> parameterSources;
	private Set<IReplacementGenerator.IParameterUsage> parameterUsages;
	private Set<AbstractInsnNode> usages;
	private Set<DataType> narrowTypeInterpretations;

	public UnpickValue(Type dataType)
	{
		this.dataType = dataType;
		this.parameterSources = new HashSet<>();
		this.parameterUsages = new HashSet<>();
		this.usages = new HashSet<>();
		this.narrowTypeInterpretations = new HashSet<>();
		if (dataType != null)
			this.addNarrowTypeInterpretationFromDesc(dataType.getDescriptor());
	}

	public UnpickValue(Type dataType, UnpickValue cloneOf)
	{
		this.dataType = dataType;
		this.parameterSources = cloneOf.getParameterSources();
		this.parameterUsages = cloneOf.getParameterUsages();
		this.usages = cloneOf.getUsages();
		this.narrowTypeInterpretations = cloneOf.getNarrowTypeInterpretations();
		if (dataType != null)
			this.addNarrowTypeInterpretationFromDesc(dataType.getDescriptor());
	}

	@Override
	public int getSize()
	{
		return dataType.getSize();
	}

	@Override
	public Type getDataType()
	{
		return dataType;
	}

	@Override
	public Set<Integer> getParameterSources()
	{
		return parameterSources;
	}

	@Override
	public Set<IReplacementGenerator.IParameterUsage> getParameterUsages()
	{
		return parameterUsages;
	}

	@Override
	public Set<AbstractInsnNode> getUsages()
	{
		return usages;
	}

	@Override
	public Set<DataType> getNarrowTypeInterpretations()
	{
		return narrowTypeInterpretations;
	}

	void setParameterSources(Set<Integer> parameterSources)
	{
		this.parameterSources = parameterSources;
	}

	void setParameterUsages(Set<IReplacementGenerator.IParameterUsage> parameterUsages)
	{
		this.parameterUsages = parameterUsages;
	}

	void setUsages(Set<AbstractInsnNode> usages)
	{
		this.usages = usages;
	}

	void setNarrowTypeInterpretations(Set<DataType> narrowTypeInterpretations)
	{
		this.narrowTypeInterpretations = narrowTypeInterpretations;
	}

	void addNarrowTypeInterpretationFromDesc(String desc)
	{
		DataType dataType;
		switch (desc)
		{
			case "C":
				dataType = DataType.CHAR;
				break;
			case "B":
				dataType = DataType.BYTE;
				break;
			case "S":
				dataType = DataType.SHORT;
				break;
			default:
				return;
		}
		getNarrowTypeInterpretations().add(dataType);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		UnpickValue that = (UnpickValue) o;

		if (!dataType.equals(that.dataType))
			return false;
		if (!parameterSources.equals(that.parameterSources))
			return false;
		if (!parameterUsages.equals(that.parameterUsages))
			return false;
		if (!usages.equals(that.usages))
			return false;
		return narrowTypeInterpretations.equals(that.narrowTypeInterpretations);
	}

	@Override
	public int hashCode()
	{
		int result = dataType.hashCode();
		result = 31 * result + parameterSources.hashCode();
		result = 31 * result + parameterUsages.hashCode();
		result = 31 * result + usages.hashCode();
		result = 31 * result + narrowTypeInterpretations.hashCode();
		return result;
	}

	public static class ParameterUsage implements IReplacementGenerator.IParameterUsage
	{
		private final AbstractInsnNode methodInvocation;
		private final int paramIndex;

		public ParameterUsage(AbstractInsnNode methodInvocation, int paramIndex)
		{
			this.methodInvocation = methodInvocation;
			this.paramIndex = paramIndex;
		}

		@Override
		public AbstractInsnNode getMethodInvocation()
		{
			return methodInvocation;
		}

		@Override
		public int getParamIndex()
		{
			return paramIndex;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			ParameterUsage that = (ParameterUsage) o;

			if (paramIndex != that.paramIndex)
				return false;
			return methodInvocation.equals(that.methodInvocation);
		}

		@Override
		public int hashCode()
		{
			int result = methodInvocation.hashCode();
			result = 31 * result + paramIndex;
			return result;
		}

		@Override
		public String toString()
		{
			return "MethodUsage{" +
					"methodInvocation=" + methodInvocation +
					", paramIndex=" + paramIndex +
					'}';
		}
	}
}

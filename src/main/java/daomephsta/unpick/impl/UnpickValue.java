package daomephsta.unpick.impl;

import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.HashSet;
import java.util.Set;

@LegacyExposed
public class UnpickValue implements IReplacementGenerator.IDataflowValue
{
	private final Type dataType;
	private final SourceValue sourceValue;
	private Set<Integer> parameterSources;
	private Set<IReplacementGenerator.IParameterUsage> methodUsages;
	private Set<AbstractInsnNode> usages;
	private Set<DataType> narrowTypeInterpretations;

	public UnpickValue(Type dataType, SourceValue sourceValue)
	{
		this.dataType = dataType;
		this.sourceValue = sourceValue;
		this.parameterSources = new HashSet<>();
		this.methodUsages = new HashSet<>();
		this.usages = new HashSet<>();
		this.narrowTypeInterpretations = new HashSet<>();
		if (dataType != null)
			this.addNarrowTypeInterpretationFromDesc(dataType.getDescriptor());
	}

	public UnpickValue(Type dataType, SourceValue sourceValue, UnpickValue cloneOf)
	{
		this.dataType = dataType;
		this.sourceValue = sourceValue;
		this.parameterSources = cloneOf.getParameterSources();
		this.methodUsages = cloneOf.getParameterUsages();
		this.usages = cloneOf.getUsages();
		this.narrowTypeInterpretations = cloneOf.getNarrowTypeInterpretations();
		if (dataType != null)
			this.addNarrowTypeInterpretationFromDesc(dataType.getDescriptor());
	}

	@Override
	public int getSize()
	{
		return sourceValue.getSize();
	}

	@Override
	public Type getDataType()
	{
		return dataType;
	}

	@LegacyExposed
	public SourceValue getSourceValue()
	{
		return sourceValue;
	}

	@LegacyExposed
	@Override
	public Set<Integer> getParameterSources()
	{
		return parameterSources;
	}

	/**
	 * @deprecated Use {@link #getParameterUsages} instead.
	 */
	@SuppressWarnings("unchecked")
	@LegacyExposed
	@Deprecated
	public Set<MethodUsage> getMethodUsages()
	{
		return (Set<MethodUsage>) (Set<?>) methodUsages;
	}

	@Override
	public Set<IReplacementGenerator.IParameterUsage> getParameterUsages()
	{
		return methodUsages;
	}

	@LegacyExposed
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

	void setParameterUsages(Set<IReplacementGenerator.IParameterUsage> methodUsages)
	{
		this.methodUsages = methodUsages;
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
		if (!sourceValue.equals(that.sourceValue))
			return false;
		if (!parameterSources.equals(that.parameterSources))
			return false;
		if (!methodUsages.equals(that.methodUsages))
			return false;
		if (!usages.equals(that.usages))
			return false;
		return narrowTypeInterpretations.equals(that.narrowTypeInterpretations);
	}

	@Override
	public int hashCode()
	{
		int result = dataType.hashCode();
		result = 31 * result + sourceValue.hashCode();
		result = 31 * result + parameterSources.hashCode();
		result = 31 * result + methodUsages.hashCode();
		result = 31 * result + usages.hashCode();
		result = 31 * result + narrowTypeInterpretations.hashCode();
		return result;
	}

	@LegacyExposed
	public static class MethodUsage implements IReplacementGenerator.IParameterUsage
	{
		private final AbstractInsnNode methodInvocation;
		private final int paramIndex;

		public MethodUsage(AbstractInsnNode methodInvocation, int paramIndex)
		{
			this.methodInvocation = methodInvocation;
			this.paramIndex = paramIndex;
		}

		@LegacyExposed
		@Override
		public AbstractInsnNode getMethodInvocation()
		{
			return methodInvocation;
		}

		@LegacyExposed
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

			MethodUsage that = (MethodUsage) o;

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

package daomephsta.unpick.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.objectweb.asm.tree.analysis.Value;

import java.util.HashSet;
import java.util.Set;

public class UnpickValue implements Value
{
	private final SourceValue sourceValue;
	private Set<Integer> parameterSources;
	private Set<MethodUsage> methodUsages;
	private Set<AbstractInsnNode> usages;

	public UnpickValue(SourceValue sourceValue)
	{
		this.sourceValue = sourceValue;
		this.parameterSources = new HashSet<>();
		this.methodUsages = new HashSet<>();
		this.usages = new HashSet<>();
	}

	public UnpickValue(SourceValue sourceValue, UnpickValue cloneOf)
	{
		this.sourceValue = sourceValue;
		this.parameterSources = cloneOf.getParameterSources();
		this.methodUsages = cloneOf.getMethodUsages();
		this.usages = cloneOf.getUsages();
	}

	@Override
	public int getSize()
	{
		return sourceValue.getSize();
	}

	public SourceValue getSourceValue()
	{
		return sourceValue;
	}

	public Set<Integer> getParameterSources()
	{
		return parameterSources;
	}

	public Set<MethodUsage> getMethodUsages()
	{
		return methodUsages;
	}

	public Set<AbstractInsnNode> getUsages()
	{
		return usages;
	}

	void setParameterSources(Set<Integer> parameterSources)
	{
		this.parameterSources = parameterSources;
	}

	void setMethodUsages(Set<MethodUsage> methodUsages)
	{
		this.methodUsages = methodUsages;
	}

	void setUsages(Set<AbstractInsnNode> usages)
	{
		this.usages = usages;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		UnpickValue that = (UnpickValue) o;

		if (!sourceValue.equals(that.sourceValue))
			return false;
		if (!parameterSources.equals(that.parameterSources))
			return false;
		if (!methodUsages.equals(that.methodUsages))
			return false;
		return usages.equals(that.usages);
	}

	@Override
	public int hashCode()
	{
		int result = sourceValue.hashCode();
		result = 31 * result + parameterSources.hashCode();
		result = 31 * result + methodUsages.hashCode();
		result = 31 * result + usages.hashCode();
		return result;
	}

	public static class MethodUsage
	{
		private final AbstractInsnNode methodInvocation;
		private final int paramIndex;

		public MethodUsage(AbstractInsnNode methodInvocation, int paramIndex)
		{
			this.methodInvocation = methodInvocation;
			this.paramIndex = paramIndex;
		}

		public AbstractInsnNode getMethodInvocation()
		{
			return methodInvocation;
		}

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

package daomephsta.unpick.impl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

public class UnpickValue implements IReplacementGenerator.IDataflowValue {
	private final Type dataType;
	private Set<Integer> parameterSources;
	private Set<IReplacementGenerator.IParameterUsage> parameterUsages;
	private Set<AbstractInsnNode> usages;
	private Set<DataType> typeInterpretations;

	public UnpickValue(Type dataType) {
		this.dataType = dataType;
		this.parameterSources = new HashSet<>();
		this.parameterUsages = new HashSet<>();
		this.usages = new HashSet<>();
		this.typeInterpretations = new HashSet<>();
		if (dataType != null) {
			this.addTypeInterpretationFromDesc(dataType.getDescriptor());
		}
	}

	public UnpickValue(Type dataType, UnpickValue cloneOf) {
		this.dataType = dataType;
		this.parameterSources = cloneOf.getParameterSources();
		this.parameterUsages = cloneOf.getParameterUsages();
		this.usages = cloneOf.getUsages();
		this.typeInterpretations = cloneOf.getTypeInterpretations();
		if (dataType != null) {
			this.addTypeInterpretationFromDesc(dataType.getDescriptor());
		}
	}

	@Override
	public int getSize() {
		return dataType.getSize();
	}

	@Override
	public Type getDataType() {
		return dataType;
	}

	@Override
	public Set<Integer> getParameterSources() {
		return parameterSources;
	}

	@Override
	public Set<IReplacementGenerator.IParameterUsage> getParameterUsages() {
		return parameterUsages;
	}

	@Override
	public Set<AbstractInsnNode> getUsages() {
		return usages;
	}

	@Override
	public Set<DataType> getTypeInterpretations() {
		return typeInterpretations;
	}

	void setParameterSources(Set<Integer> parameterSources) {
		this.parameterSources = parameterSources;
	}

	void setParameterUsages(Set<IReplacementGenerator.IParameterUsage> parameterUsages) {
		this.parameterUsages = parameterUsages;
	}

	void setUsages(Set<AbstractInsnNode> usages) {
		this.usages = usages;
	}

	void setTypeInterpretations(Set<DataType> typeInterpretations) {
		this.typeInterpretations = typeInterpretations;
	}

	void addTypeInterpretationFromDesc(String desc) {
		DataType dataType;
		switch (desc) {
			case "C":
				dataType = DataType.CHAR;
				break;
			case "B":
				dataType = DataType.BYTE;
				break;
			case "S":
				dataType = DataType.SHORT;
				break;
			case "I":
				dataType = DataType.INT;
				break;
			case "J":
				dataType = DataType.LONG;
				break;
			case "F":
				dataType = DataType.FLOAT;
				break;
			case "D":
				dataType = DataType.DOUBLE;
				break;
			case "Ljava/lang/String;":
				dataType = DataType.STRING;
				break;
			case "Ljava/lang/Class;":
				dataType = DataType.CLASS;
				break;
			default:
				return;
		}
		typeInterpretations.add(dataType);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		UnpickValue that = (UnpickValue) o;

		if (!Objects.equals(dataType, that.dataType)) {
			return false;
		}
		if (!parameterSources.equals(that.parameterSources)) {
			return false;
		}
		if (!parameterUsages.equals(that.parameterUsages)) {
			return false;
		}
		if (!usages.equals(that.usages)) {
			return false;
		}
		return typeInterpretations.equals(that.typeInterpretations);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(dataType);
		result = 31 * result + parameterSources.hashCode();
		result = 31 * result + parameterUsages.hashCode();
		result = 31 * result + usages.hashCode();
		result = 31 * result + typeInterpretations.hashCode();
		return result;
	}

	public static class ParameterUsage implements IReplacementGenerator.IParameterUsage {
		private final AbstractInsnNode methodInvocation;
		private final int paramIndex;

		public ParameterUsage(AbstractInsnNode methodInvocation, int paramIndex) {
			this.methodInvocation = methodInvocation;
			this.paramIndex = paramIndex;
		}

		@Override
		public AbstractInsnNode getMethodInvocation() {
			return methodInvocation;
		}

		@Override
		public int getParamIndex() {
			return paramIndex;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ParameterUsage that = (ParameterUsage) o;

			if (paramIndex != that.paramIndex) {
				return false;
			}
			return methodInvocation.equals(that.methodInvocation);
		}

		@Override
		public int hashCode() {
			int result = methodInvocation.hashCode();
			result = 31 * result + paramIndex;
			return result;
		}

		@Override
		public String toString() {
			return "MethodUsage{"
					+ "methodInvocation=" + methodInvocation
					+ ", paramIndex=" + paramIndex
					+ '}';
		}
	}
}

package daomephsta.unpick.impl;

import daomephsta.unpick.api.inheritancecheckers.IInheritanceChecker;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnpickInterpreter extends Interpreter<UnpickValue> implements Opcodes
{
	private final MethodNode method;
	private final IInheritanceChecker inheritanceChecker;
	private final BasicInterpreter typeTracker = new BasicInterpreter()
	{
		@Override
		public BasicValue newValue(Type type)
		{
			if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)
			{
				return new BasicValue(type);
			}

			return super.newValue(type);
		}

		@Override
		public BasicValue merge(BasicValue value1, BasicValue value2)
		{
			if (!value1.equals(value2))
			{
				Type type1 = value1.getType();
				Type type2 = value2.getType();
				if (type1 != null
					&& (type1.getSort() == Type.OBJECT || type1.getSort() == Type.ARRAY)
					&& type2 != null
					&& (type2.getSort() == Type.OBJECT || type2.getSort() == Type.ARRAY))
				{
					if (type1.equals(NULL_TYPE))
						return value2;
					if (type2.equals(NULL_TYPE))
						return value1;
					if (isAssignableFrom(type1, type2))
						return value1;
					if (isAssignableFrom(type2, type1))
						return value2;
					int numDimensions = 0;
					if (type1.getSort() == Type.ARRAY
						&& type2.getSort() == Type.ARRAY
						&& type1.getDimensions() == type2.getDimensions()
						&& type1.getElementType().getSort() == Type.OBJECT
						&& type2.getElementType().getSort() == Type.OBJECT)
					{
						numDimensions = type1.getDimensions();
						type1 = type1.getElementType();
						type2 = type2.getElementType();
					}
					while (true)
					{
						if (type1 == null || isInterface(type1))
							return newArrayValue(Type.getObjectType("java/lang/Object"), numDimensions);
						type1 = getSuperClass(type1);
						if (type1 != null && isAssignableFrom(type1, type2))
						{
							return newArrayValue(type1, numDimensions);
						}
					}
				}

				return BasicValue.UNINITIALIZED_VALUE;
			}

			return value1;
		}

		private boolean isAssignableFrom(Type type1, Type type2)
		{
			if (type1.equals(type2))
			{
				return true;
			}
			if ((type1.getSort() != Type.OBJECT && type1.getSort() != Type.ARRAY) || (type2.getSort() != Type.OBJECT && type2.getSort() != Type.ARRAY))
			{
				return false;
			}
			if (type1.getInternalName().equals("java/lang/Object"))
			{
				return true;
			}

			if (type2.getSort() == Type.ARRAY)
			{
				if (type1.getSort() == Type.OBJECT)
				{
					return type1.getInternalName().equals("java/lang/Object");
				}
				int dimensions1 = type1.getDimensions();
				int dimensions2 = type2.getDimensions();
				if (dimensions1 < dimensions2)
				{
					return type1.getElementType().getDescriptor().equals("Ljava/lang/Object;");
				}
				else if (dimensions1 > dimensions2)
				{
					return false;
				}
				else
				{
					return isAssignableFrom(type1.getElementType(), type2.getElementType());
				}
			}
			else if (type1.getSort() == Type.ARRAY)
			{
				return false;
			}
			else
			{
				return inheritanceChecker.isAssignableFrom(type1.getInternalName(), type2.getInternalName());
			}
		}

		@Nullable
		private Type getSuperClass(Type type)
		{
			if (type.getSort() == Type.ARRAY)
			{
				return Type.getObjectType("java/lang/Object");
			}
			else
			{
				if ("java/lang/Object".equals(type.getInternalName()))
				{
					return null;
				}

				IInheritanceChecker.ClassInfo classInfo = inheritanceChecker.getClassInfo(type.getInternalName());
				if (classInfo == null)
				{
					return Type.getObjectType("java/lang/Object");
				}
				else
				{
					return Type.getObjectType(Objects.requireNonNull(classInfo.getSuperClass(), "returned null for non-Object superclass"));
				}
			}
		}

		private boolean isInterface(Type type)
		{
			if (type.getSort() == Type.OBJECT)
			{
				return false;
			}

			IInheritanceChecker.ClassInfo classInfo = inheritanceChecker.getClassInfo(type.getInternalName());
			return classInfo != null && classInfo.isInterface();
		}

		private BasicValue newArrayValue(Type type, int dimensions)
		{
			if (dimensions == 0)
			{
				return newValue(type);
			}
			else
			{
				StringBuilder desc = new StringBuilder(type.getDescriptor().length() + dimensions);
				for (int i = 0; i < dimensions; i++)
				{
					desc.append('[');
				}
				desc.append(type.getDescriptor());
				return newValue(Type.getType(desc.toString()));
			}
		}
	};
	private final SourceInterpreter delegate = new SourceInterpreter();

	public UnpickInterpreter(MethodNode method, IInheritanceChecker inheritanceChecker)
	{
		super(ASM9);
		this.method = method;
		this.inheritanceChecker = inheritanceChecker;
	}

	@Override
	public UnpickValue newParameterValue(boolean isInstanceMethod, int local, Type type)
	{
		UnpickValue value = newValue(type);
		int localIndex = isInstanceMethod ? 1 : 0;
		int paramIndex = 0;
		for (Type argument : Type.getArgumentTypes(method.desc))
		{
			if (localIndex == local)
				break;
			localIndex += argument.getSize();
			paramIndex++;
		}
		value.getParameterSources().add(paramIndex);
		return value;
	}

	@Override
	public UnpickValue newValue(Type type)
	{
		if (type == Type.VOID_TYPE)
			return null;
		return new UnpickValue(type, delegate.newValue(type));
	}

	@Override
	public UnpickValue newOperation(AbstractInsnNode insn) throws AnalyzerException
	{
		Type type = typeTracker.newOperation(insn).getType();
		SourceValue sourceValue = delegate.newOperation(insn);
		UnpickValue value = new UnpickValue(type, sourceValue);
		value.getUsages().add(insn);
		return value;
	}

	@Override
	public UnpickValue copyOperation(AbstractInsnNode insn, UnpickValue value) throws AnalyzerException
	{
		Type type = typeTracker.copyOperation(insn, typeTracker.newValue(value.getDataType())).getType();
		SourceValue sourceValue = delegate.copyOperation(insn, value.getSourceValue());
		return new UnpickValue(type, sourceValue, value);
	}

	@Override
	public UnpickValue unaryOperation(AbstractInsnNode insn, UnpickValue value) throws AnalyzerException
	{
		Type type = typeTracker.unaryOperation(insn, typeTracker.newValue(value.getDataType())).getType();
		SourceValue sourceValue = delegate.unaryOperation(insn, value.getSourceValue());
		UnpickValue newValue = new UnpickValue(type, sourceValue, value);
		if (insn.getType() == AbstractInsnNode.FIELD_INSN || insn.getType() == AbstractInsnNode.JUMP_INSN || (insn.getOpcode() >= IRETURN && insn.getOpcode() <= RETURN))
		{
			newValue.getUsages().add(insn);
		}
		return newValue;
	}

	@Override
	public UnpickValue binaryOperation(AbstractInsnNode insn, UnpickValue value1, UnpickValue value2) throws AnalyzerException
	{
		Type type = typeTracker.binaryOperation(insn, typeTracker.newValue(value1.getDataType()), typeTracker.newValue(value2.getDataType())).getType();
		SourceValue sourceValue = delegate.binaryOperation(insn, value1.getSourceValue(), value2.getSourceValue());
		switch (insn.getOpcode())
		{
			case IALOAD:
			case FALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case LALOAD:
			case DALOAD:
				return new UnpickValue(type, sourceValue);
			case IADD:
			case LADD:
			case FADD:
			case DADD:
			case ISUB:
			case LSUB:
			case FSUB:
			case DSUB:
			case IMUL:
			case LMUL:
			case FMUL:
			case DMUL:
			case IDIV:
			case LDIV:
			case FDIV:
			case DDIV:
			case IREM:
			case LREM:
			case FREM:
			case DREM:
			case IAND:
			case LAND:
			case IOR:
			case LOR:
			case IXOR:
			case LXOR:
				return new UnpickValue(type, sourceValue, merge(value1, value2));
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
				return new UnpickValue(type, sourceValue, value1);
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
				merge(value1, value2);
				return new UnpickValue(type, sourceValue);
			case PUTFIELD:
				value2.getUsages().add(insn);
				return new UnpickValue(type, sourceValue);
			default:
				throw new IllegalArgumentException("Unrecognized insn: " + insn.getOpcode());
		}
	}

	@Override
	public UnpickValue ternaryOperation(AbstractInsnNode insn, UnpickValue value1, UnpickValue value2, UnpickValue value3) throws AnalyzerException
	{
		Type type = typeTracker.ternaryOperation(insn, typeTracker.newValue(value1.getDataType()), typeTracker.newValue(value2.getDataType()), typeTracker.newValue(value3.getDataType())).getType();
		SourceValue sourceValue = delegate.ternaryOperation(insn, value1.getSourceValue(), value2.getSourceValue(), value3.getSourceValue());
		return new UnpickValue(type, sourceValue);
	}

	@Override
	public UnpickValue naryOperation(AbstractInsnNode insn, List<? extends UnpickValue> values) throws AnalyzerException
	{
		Type type = typeTracker.naryOperation(insn, values.stream().map(value -> typeTracker.newValue(value.getDataType())).collect(Collectors.toList())).getType();
		SourceValue sourceValue = delegate.naryOperation(insn, values.stream().map(UnpickValue::getSourceValue).collect(Collectors.toList()));
		if (insn.getOpcode() == MULTIANEWARRAY)
		{
			return new UnpickValue(type, sourceValue);
		}
		else
		{
			boolean hasThis = insn.getOpcode() != INVOKESTATIC && insn.getOpcode() != INVOKEDYNAMIC;
			for (int i = hasThis ? 1 : 0; i < values.size(); i++)
			{
				values.get(i).getParameterUsages().add(new UnpickValue.MethodUsage(insn, hasThis ? i - 1 : i));
			}
			UnpickValue value = new UnpickValue(type, sourceValue);
			value.getUsages().add(insn);
			return value;
		}
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, UnpickValue value, UnpickValue expected)
	{
		// Already handled in unaryOperation
	}

	@Override
	public UnpickValue merge(UnpickValue value1, UnpickValue value2)
	{
		value1.getParameterSources().addAll(value2.getParameterSources());
		value1.getParameterUsages().addAll(value2.getParameterUsages());
		value1.getUsages().addAll(value2.getUsages());
		value2.setParameterSources(value1.getParameterSources());
		value2.setParameterUsages(value1.getParameterUsages());
		value2.setUsages(value1.getUsages());
		Type type = typeTracker.merge(typeTracker.newValue(value1.getDataType()), typeTracker.newValue(value2.getDataType())).getType();
		SourceValue sourceValue = delegate.merge(value1.getSourceValue(), value2.getSourceValue());
		return new UnpickValue(type, sourceValue, value1);
	}
}

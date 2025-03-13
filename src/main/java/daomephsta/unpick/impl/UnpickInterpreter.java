package daomephsta.unpick.impl;

import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

import org.jetbrains.annotations.Nullable;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnpickInterpreter extends Interpreter<UnpickValue> implements Opcodes
{
	private static final BasicValue BYTE_VALUE = new BasicValue(Type.BYTE_TYPE);
	private static final BasicValue SHORT_VALUE = new BasicValue(Type.SHORT_TYPE);
	private static final BasicValue CHAR_VALUE = new BasicValue(Type.CHAR_TYPE);

	private final MethodNode method;
	private final IInheritanceChecker inheritanceChecker;
	private final BasicInterpreter typeTracker = new BasicInterpreter(Opcodes.ASM9)
	{
		@Override
		public BasicValue newValue(Type type)
		{
			switch (type.getSort())
			{
				case Type.OBJECT:
				case Type.ARRAY:
					return new BasicValue(type);
				case Type.BYTE:
					return BYTE_VALUE;
				case Type.SHORT:
					return SHORT_VALUE;
				case Type.CHAR:
					return CHAR_VALUE;
				default:
					return super.newValue(type);
			}
		}

		@Override
		public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException
		{
			switch (insn.getOpcode())
			{
				case I2B:
					return BYTE_VALUE;
				case I2S:
					return SHORT_VALUE;
				case I2C:
					return CHAR_VALUE;
				default:
					return super.unaryOperation(insn, value);
			}
		}

		@Override
		public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException
		{
			switch (insn.getOpcode())
			{
				case BALOAD:
					if (value1.getType().getDescriptor().equals("[Z"))
					{
						return super.binaryOperation(insn, value1, value2);
					}
					else
					{
						return BYTE_VALUE;
					}
				case SALOAD:
					return SHORT_VALUE;
				case CALOAD:
					return CHAR_VALUE;
				default:
					return super.binaryOperation(insn, value1, value2);
			}
		}

		@Override
		public BasicValue merge(BasicValue value1, BasicValue value2)
		{
			if (value1.equals(value2))
			{
				return value1;
			}

			Type type1 = value1.getType();
			Type type2 = value2.getType();
			if (type1 == null || type2 == null)
			{
				return BasicValue.UNINITIALIZED_VALUE;
			}

			boolean isIntegral1 = type1.getSort() >= Type.CHAR && type1.getSort() <= Type.INT;
			boolean isIntegral2 = type2.getSort() >= Type.CHAR && type2.getSort() <= Type.INT;
			if (isIntegral1 && isIntegral2)
			{
				if (type1 == Type.CHAR_TYPE || type2 == Type.CHAR_TYPE)
				{
					return BasicValue.INT_VALUE;
				}

				return type1.getSort() > type2.getSort() ? value1 : value2;
			}

			boolean isReference1 = type1.getSort() == Type.OBJECT || type1.getSort() == Type.ARRAY;
			boolean isReference2 = type2.getSort() == Type.OBJECT || type2.getSort() == Type.ARRAY;
			if (isReference1 && isReference2)
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
		return new UnpickValue(getType(typeTracker.newValue(type)));
	}

	@Override
	public UnpickValue newOperation(AbstractInsnNode insn) throws AnalyzerException
	{
		UnpickValue value = new UnpickValue(getType(typeTracker.newOperation(insn)));
		value.getUsages().add(insn);
		return value;
	}

	@Override
	public UnpickValue copyOperation(AbstractInsnNode insn, UnpickValue value) throws AnalyzerException
	{
		Type type = getType(typeTracker.copyOperation(insn, typeTracker.newValue(value.getDataType())));
		return new UnpickValue(type, value);
	}

	@Override
	public UnpickValue unaryOperation(AbstractInsnNode insn, UnpickValue value) throws AnalyzerException
	{
		switch (insn.getOpcode())
		{
			case INEG:
			case IINC:
			case I2L:
			case I2F:
			case I2D:
			case I2B:
			case I2C:
			case I2S:
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case NEWARRAY:
			case ANEWARRAY:
				value.getTypeInterpretations().add(DataType.INT);
				break;
			case LNEG:
			case L2I:
			case L2F:
			case L2D:
				value.getTypeInterpretations().add(DataType.LONG);
				break;
			case FNEG:
			case F2I:
			case F2L:
			case F2D:
				value.getTypeInterpretations().add(DataType.FLOAT);
				break;
			case DNEG:
			case D2I:
			case D2L:
			case D2F:
				value.getTypeInterpretations().add(DataType.DOUBLE);
				break;
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
				value.addTypeInterpretationFromDesc(Type.getReturnType(method.desc).getDescriptor());
				break;
			case PUTSTATIC:
				value.addTypeInterpretationFromDesc(((FieldInsnNode) insn).desc);
				break;
			case CHECKCAST:
				value.addTypeInterpretationFromDesc(((TypeInsnNode) insn).desc);
				break;
		}

		Type type = getType(typeTracker.unaryOperation(insn, typeTracker.newValue(value.getDataType())));
		UnpickValue newValue = new UnpickValue(type, value);
		if (insn.getType() == AbstractInsnNode.FIELD_INSN || insn.getType() == AbstractInsnNode.JUMP_INSN || (insn.getOpcode() >= IRETURN && insn.getOpcode() <= RETURN))
		{
			newValue.getUsages().add(insn);
		}
		return newValue;
	}

	@Override
	public UnpickValue binaryOperation(AbstractInsnNode insn, UnpickValue value1, UnpickValue value2) throws AnalyzerException
	{
		Type type = getType(typeTracker.binaryOperation(insn, typeTracker.newValue(value1.getDataType()), typeTracker.newValue(value2.getDataType())));
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
				value2.getTypeInterpretations().add(DataType.INT);
				return new UnpickValue(type);
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
				return new UnpickValue(type, merge(value1, value2));
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
				value2.getTypeInterpretations().add(DataType.INT);
				return new UnpickValue(type, value1);
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
				return new UnpickValue(type);
			case PUTFIELD:
				value2.getUsages().add(insn);
				value2.addTypeInterpretationFromDesc(((FieldInsnNode) insn).desc);
				return new UnpickValue(type);
			default:
				throw new IllegalArgumentException("Unrecognized insn: " + insn.getOpcode());
		}
	}

	@Override
	public UnpickValue ternaryOperation(AbstractInsnNode insn, UnpickValue value1, UnpickValue value2, UnpickValue value3) throws AnalyzerException
	{
		// only used for arrays
		value2.getTypeInterpretations().add(DataType.INT);

		switch (insn.getOpcode())
		{
			case BASTORE:
				if (!value1.getDataType().getDescriptor().equals("[Z"))
				{
					value3.getTypeInterpretations().add(DataType.BYTE);
				}
				break;
			case SASTORE:
				value3.getTypeInterpretations().add(DataType.SHORT);
				break;
			case CASTORE:
				value3.getTypeInterpretations().add(DataType.CHAR);
				break;
			case IASTORE:
				value3.getTypeInterpretations().add(DataType.INT);
				break;
			case LASTORE:
				value3.getTypeInterpretations().add(DataType.LONG);
				break;
			case FASTORE:
				value3.getTypeInterpretations().add(DataType.FLOAT);
				break;
			case DASTORE:
				value3.getTypeInterpretations().add(DataType.DOUBLE);
				break;
			case AASTORE:
				if (value1.getDataType().getSort() == Type.ARRAY)
				{
					value3.addTypeInterpretationFromDesc(value1.getDataType().getDescriptor().substring(1));
				}
				break;
		}

		Type type = getType(typeTracker.ternaryOperation(insn, typeTracker.newValue(value1.getDataType()), typeTracker.newValue(value2.getDataType()), typeTracker.newValue(value3.getDataType())));
		return new UnpickValue(type);
	}

	@Override
	public UnpickValue naryOperation(AbstractInsnNode insn, List<? extends UnpickValue> values) throws AnalyzerException
	{
		Type type = getType(typeTracker.naryOperation(insn, values.stream().map(value -> typeTracker.newValue(value.getDataType())).collect(Collectors.toList())));
		if (insn.getOpcode() == MULTIANEWARRAY)
		{
			return new UnpickValue(type);
		}
		else
		{
			boolean hasThis = insn.getOpcode() != INVOKESTATIC && insn.getOpcode() != INVOKEDYNAMIC;
			String desc = insn.getOpcode() == INVOKEDYNAMIC ? ((InvokeDynamicInsnNode) insn).desc : ((MethodInsnNode) insn).desc;
			Type[] argumentTypes = Type.getArgumentTypes(desc);
			for (int i = hasThis ? 1 : 0; i < values.size(); i++)
			{
				int paramIndex = hasThis ? i - 1 : i;
				values.get(i).getParameterUsages().add(new UnpickValue.ParameterUsage(insn, paramIndex));
				values.get(i).addTypeInterpretationFromDesc(argumentTypes[paramIndex].getDescriptor());
			}
			UnpickValue value = new UnpickValue(type);
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
		value1.getTypeInterpretations().addAll(value2.getTypeInterpretations());
		value2.setParameterSources(value1.getParameterSources());
		value2.setParameterUsages(value1.getParameterUsages());
		value2.setUsages(value1.getUsages());
		value2.setTypeInterpretations(value1.getTypeInterpretations());
		Type type = typeTracker.merge(typeTracker.newValue(value1.getDataType()), typeTracker.newValue(value2.getDataType())).getType();
		return new UnpickValue(type, value1);
	}

	private static Type getType(BasicValue value)
	{
		return value == null ? null : value.getType();
	}
}

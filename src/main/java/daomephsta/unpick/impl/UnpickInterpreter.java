package daomephsta.unpick.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

public class UnpickInterpreter extends Interpreter<UnpickValue> implements Opcodes {
	private static final BasicValue BYTE_VALUE = new BasicValue(Type.BYTE_TYPE);
	private static final BasicValue SHORT_VALUE = new BasicValue(Type.SHORT_TYPE);
	private static final BasicValue CHAR_VALUE = new BasicValue(Type.CHAR_TYPE);

	private final MethodNode method;
	private final IInheritanceChecker inheritanceChecker;
	private final BasicInterpreter typeTracker = new BasicInterpreter(Opcodes.ASM9) {
		@Override
		public BasicValue newValue(Type type) {
			if (type == null) {
				return super.newValue(null);
			}
			return switch (type.getSort()) {
				case Type.OBJECT, Type.ARRAY -> new BasicValue(type);
				case Type.BYTE -> BYTE_VALUE;
				case Type.SHORT -> SHORT_VALUE;
				case Type.CHAR -> CHAR_VALUE;
				default -> super.newValue(type);
			};
		}

		@Override
		public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
			return switch (insn.getOpcode()) {
				case I2B -> BYTE_VALUE;
				case I2S -> SHORT_VALUE;
				case I2C -> CHAR_VALUE;
				default -> super.unaryOperation(insn, value);
			};
		}

		@Override
		public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
			return switch (insn.getOpcode()) {
				case BALOAD -> {
					if (value1.getType().getDescriptor().equals("[Z")) {
						yield super.binaryOperation(insn, value1, value2);
					} else {
						yield BYTE_VALUE;
					}
				}
				case SALOAD -> SHORT_VALUE;
				case CALOAD -> CHAR_VALUE;
				default -> super.binaryOperation(insn, value1, value2);
			};
		}

		@Override
		public BasicValue merge(BasicValue value1, BasicValue value2) {
			if (value1.equals(value2)) {
				return value1;
			}

			Type type1 = value1.getType();
			Type type2 = value2.getType();
			if (type1 == null || type2 == null) {
				return BasicValue.UNINITIALIZED_VALUE;
			}

			boolean isIntegral1 = type1.getSort() >= Type.CHAR && type1.getSort() <= Type.INT;
			boolean isIntegral2 = type2.getSort() >= Type.CHAR && type2.getSort() <= Type.INT;
			if (isIntegral1 && isIntegral2) {
				if (type1 == Type.CHAR_TYPE || type2 == Type.CHAR_TYPE) {
					return BasicValue.INT_VALUE;
				}

				return type1.getSort() > type2.getSort() ? value1 : value2;
			}

			boolean isReference1 = type1.getSort() == Type.OBJECT || type1.getSort() == Type.ARRAY;
			boolean isReference2 = type2.getSort() == Type.OBJECT || type2.getSort() == Type.ARRAY;
			if (isReference1 && isReference2) {
				if (type1.equals(NULL_TYPE)) {
					return value2;
				}
				if (type2.equals(NULL_TYPE)) {
					return value1;
				}
				if (isAssignableFrom(type1, type2)) {
					return value1;
				}
				if (isAssignableFrom(type2, type1)) {
					return value2;
				}
				int numDimensions = 0;
				if (type1.getSort() == Type.ARRAY
						&& type2.getSort() == Type.ARRAY
						&& type1.getDimensions() == type2.getDimensions()
						&& type1.getElementType().getSort() == Type.OBJECT
						&& type2.getElementType().getSort() == Type.OBJECT) {
					numDimensions = type1.getDimensions();
					type1 = type1.getElementType();
					type2 = type2.getElementType();
				}
				while (true) {
					if (type1 == null || isInterface(type1)) {
						return newArrayValue(Type.getObjectType("java/lang/Object"), numDimensions);
					}
					type1 = getSuperClass(type1);
					if (type1 != null && isAssignableFrom(type1, type2)) {
						return newArrayValue(type1, numDimensions);
					}
				}
			}

			return BasicValue.UNINITIALIZED_VALUE;
		}

		private boolean isAssignableFrom(Type type1, Type type2) {
			if (type1.equals(type2)) {
				return true;
			}
			if ((type1.getSort() != Type.OBJECT && type1.getSort() != Type.ARRAY) || (type2.getSort() != Type.OBJECT && type2.getSort() != Type.ARRAY)) {
				return false;
			}
			if (type1.getInternalName().equals("java/lang/Object")) {
				return true;
			}

			if (type2.getSort() == Type.ARRAY) {
				if (type1.getSort() == Type.OBJECT) {
					return type1.getInternalName().equals("java/lang/Object");
				}
				int dimensions1 = type1.getDimensions();
				int dimensions2 = type2.getDimensions();
				if (dimensions1 < dimensions2) {
					return type1.getElementType().getDescriptor().equals("Ljava/lang/Object;");
				} else if (dimensions1 > dimensions2) {
					return false;
				} else {
					return isAssignableFrom(type1.getElementType(), type2.getElementType());
				}
			} else if (type1.getSort() == Type.ARRAY) {
				return false;
			} else {
				return inheritanceChecker.isAssignableFrom(type1.getInternalName(), type2.getInternalName());
			}
		}

		@Nullable
		private Type getSuperClass(Type type) {
			if (type.getSort() == Type.ARRAY) {
				return Type.getObjectType("java/lang/Object");
			} else {
				if ("java/lang/Object".equals(type.getInternalName())) {
					return null;
				}

				IInheritanceChecker.ClassInfo classInfo = inheritanceChecker.getClassInfo(type.getInternalName());
				if (classInfo == null) {
					return Type.getObjectType("java/lang/Object");
				} else {
					return Type.getObjectType(Objects.requireNonNull(classInfo.superClass(), "returned null for non-Object superclass"));
				}
			}
		}

		private boolean isInterface(Type type) {
			if (type.getSort() == Type.OBJECT) {
				return false;
			}

			IInheritanceChecker.ClassInfo classInfo = inheritanceChecker.getClassInfo(type.getInternalName());
			return classInfo != null && classInfo.isInterface();
		}

		private BasicValue newArrayValue(Type type, int dimensions) {
			if (dimensions == 0) {
				return newValue(type);
			} else {
				StringBuilder desc = new StringBuilder(type.getDescriptor().length() + dimensions);
				for (int i = 0; i < dimensions; i++) {
					desc.append('[');
				}
				desc.append(type.getDescriptor());
				return newValue(Type.getType(desc.toString()));
			}
		}
	};

	public UnpickInterpreter(MethodNode method, IInheritanceChecker inheritanceChecker) {
		super(ASM9);
		this.method = method;
		this.inheritanceChecker = inheritanceChecker;
	}

	@Override
	public UnpickValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
		UnpickValue value = newValue(type);
		int localIndex = isInstanceMethod ? 1 : 0;
		int paramIndex = 0;
		for (Type argument : Type.getArgumentTypes(method.desc)) {
			if (localIndex == local) {
				break;
			}
			localIndex += argument.getSize();
			paramIndex++;
		}
		value.getParameterSources().add(paramIndex);
		return value;
	}

	@Override
	public UnpickValue newValue(Type type) {
		if (type == Type.VOID_TYPE) {
			return null;
		}
		return new UnpickValue(getType(typeTracker.newValue(type)));
	}

	@Override
	public UnpickValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		UnpickValue value = new UnpickValue(getType(typeTracker.newOperation(insn)));
		value.getUsages().add(insn);
		return value;
	}

	@Override
	public UnpickValue copyOperation(AbstractInsnNode insn, UnpickValue value) throws AnalyzerException {
		Type type = getType(typeTracker.copyOperation(insn, typeTracker.newValue(value.getDataType())));
		return new UnpickValue(type, value);
	}

	@Override
	public UnpickValue unaryOperation(AbstractInsnNode insn, UnpickValue value) throws AnalyzerException {
		switch (insn.getOpcode()) {
			case INEG,
				IINC,
				I2L,
				I2F,
				I2D,
				I2B,
				I2C,
				I2S,
				IFEQ,
				IFNE,
				IFLT,
				IFGE,
				IFGT,
				IFLE,
				TABLESWITCH,
				LOOKUPSWITCH,
				NEWARRAY,
				ANEWARRAY -> value.getTypeInterpretations().add(DataType.INT);
			case LNEG, L2I, L2F, L2D -> value.getTypeInterpretations().add(DataType.LONG);
			case FNEG, F2I, F2L, F2D -> value.getTypeInterpretations().add(DataType.FLOAT);
			case DNEG, D2I, D2L, D2F -> value.getTypeInterpretations().add(DataType.DOUBLE);
			case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> value.addTypeInterpretationFromType(Type.getReturnType(method.desc));
			case PUTSTATIC -> value.addTypeInterpretationFromType(Type.getType(((FieldInsnNode) insn).desc));
			case CHECKCAST -> value.addTypeInterpretationFromType(Type.getObjectType(((TypeInsnNode) insn).desc));
		}

		Type type = getType(typeTracker.unaryOperation(insn, typeTracker.newValue(value.getDataType())));
		UnpickValue newValue = new UnpickValue(type, value);
		if (insn.getType() == AbstractInsnNode.FIELD_INSN || insn.getType() == AbstractInsnNode.JUMP_INSN || (insn.getOpcode() >= IRETURN && insn.getOpcode() <= RETURN)) {
			newValue.getUsages().add(insn);
		}
		return newValue;
	}

	@Override
	public UnpickValue binaryOperation(AbstractInsnNode insn, UnpickValue value1, UnpickValue value2) throws AnalyzerException {
		Type type = getType(typeTracker.binaryOperation(insn, typeTracker.newValue(value1.getDataType()), typeTracker.newValue(value2.getDataType())));
		return switch (insn.getOpcode()) {
			case IALOAD,
				FALOAD,
				AALOAD,
				BALOAD,
				CALOAD,
				SALOAD,
				LALOAD,
				DALOAD -> {
				value2.getTypeInterpretations().add(DataType.INT);
				yield new UnpickValue(type);
			}
			case IADD,
				LADD,
				FADD,
				DADD,
				ISUB,
				LSUB,
				FSUB,
				DSUB,
				IMUL,
				LMUL,
				FMUL,
				DMUL,
				IDIV,
				LDIV,
				FDIV,
				DDIV,
				IREM,
				LREM,
				FREM,
				DREM,
				IAND,
				LAND,
				IOR,
				LOR,
				IXOR,
				LXOR -> new UnpickValue(type, merge(value1, value2));
			case ISHL,
				LSHL,
				ISHR,
				LSHR,
				IUSHR,
				LUSHR -> {
				value2.getTypeInterpretations().add(DataType.INT);
				yield new UnpickValue(type, value1);
			}
			case LCMP,
				FCMPL,
				FCMPG,
				DCMPL,
				DCMPG,
				IF_ICMPEQ,
				IF_ICMPNE,
				IF_ICMPLT,
				IF_ICMPGE,
				IF_ICMPGT,
				IF_ICMPLE,
				IF_ACMPEQ,
				IF_ACMPNE -> {
				merge(value1, value2);
				yield new UnpickValue(type);
			}
			case PUTFIELD -> {
				value2.getUsages().add(insn);
				value2.addTypeInterpretationFromType(Type.getType(((FieldInsnNode) insn).desc));
				yield new UnpickValue(type);
			}
			default ->
				throw new IllegalArgumentException("Unrecognized insn: " + insn.getOpcode());
		};
	}

	@Override
	public UnpickValue ternaryOperation(AbstractInsnNode insn, UnpickValue value1, UnpickValue value2, UnpickValue value3) throws AnalyzerException {
		// only used for arrays
		value2.getTypeInterpretations().add(DataType.INT);

		switch (insn.getOpcode()) {
			case BASTORE -> {
				if (!value1.getDataType().getDescriptor().equals("[Z")) {
					value3.getTypeInterpretations().add(DataType.BYTE);
				}
			}
			case SASTORE -> value3.getTypeInterpretations().add(DataType.SHORT);
			case CASTORE -> value3.getTypeInterpretations().add(DataType.CHAR);
			case IASTORE -> value3.getTypeInterpretations().add(DataType.INT);
			case LASTORE -> value3.getTypeInterpretations().add(DataType.LONG);
			case FASTORE -> value3.getTypeInterpretations().add(DataType.FLOAT);
			case DASTORE -> value3.getTypeInterpretations().add(DataType.DOUBLE);
			case AASTORE -> {
				if (value1.getDataType().getSort() == Type.ARRAY) {
					value3.addTypeInterpretationFromType(Type.getType(value1.getDataType().getDescriptor().substring(1)));
				}
			}
		}

		Type type = getType(typeTracker.ternaryOperation(insn, typeTracker.newValue(value1.getDataType()), typeTracker.newValue(value2.getDataType()), typeTracker.newValue(value3.getDataType())));
		return new UnpickValue(type);
	}

	@Override
	public UnpickValue naryOperation(AbstractInsnNode insn, List<? extends UnpickValue> values) throws AnalyzerException {
		Type type = getType(typeTracker.naryOperation(insn, values.stream().map(value -> typeTracker.newValue(value.getDataType())).collect(Collectors.toList())));
		if (insn.getOpcode() == MULTIANEWARRAY) {
			return new UnpickValue(type);
		} else {
			boolean hasThis = insn.getOpcode() != INVOKESTATIC && insn.getOpcode() != INVOKEDYNAMIC;
			String desc = insn.getOpcode() == INVOKEDYNAMIC ? ((InvokeDynamicInsnNode) insn).desc : ((MethodInsnNode) insn).desc;
			Type[] argumentTypes = Type.getArgumentTypes(desc);
			for (int i = hasThis ? 1 : 0; i < values.size(); i++) {
				int paramIndex = hasThis ? i - 1 : i;
				values.get(i).getParameterUsages().add(new UnpickValue.ParameterUsage(insn, paramIndex));
				values.get(i).addTypeInterpretationFromType(argumentTypes[paramIndex]);
			}
			UnpickValue value = new UnpickValue(type);
			value.getUsages().add(insn);
			return value;
		}
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, UnpickValue value, UnpickValue expected) {
		// Already handled in unaryOperation
	}

	@Override
	public UnpickValue merge(UnpickValue value1, UnpickValue value2) {
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

	private static Type getType(BasicValue value) {
		return value == null ? null : value.getType();
	}
}

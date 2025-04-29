package daomephsta.unpick.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

public class UnpickInterpreter extends Interpreter<UnpickValue> implements Opcodes {
	private final MethodNode method;
	private final SourceInterpreter delegate = new SourceInterpreter();

	public UnpickInterpreter(MethodNode method) {
		super(ASM9);
		this.method = method;
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
		return new UnpickValue(delegate.newValue(type));
	}

	@Override
	public UnpickValue newOperation(AbstractInsnNode insn) {
		UnpickValue value = new UnpickValue(delegate.newOperation(insn));
		value.getUsages().add(insn);
		return value;
	}

	@Override
	public UnpickValue copyOperation(AbstractInsnNode insn, UnpickValue value) {
		return new UnpickValue(delegate.copyOperation(insn, value.getSourceValue()), value);
	}

	@Override
	public UnpickValue unaryOperation(AbstractInsnNode insn, UnpickValue value) {
		UnpickValue newValue = new UnpickValue(delegate.unaryOperation(insn, value.getSourceValue()), value);
		if (insn.getType() == AbstractInsnNode.FIELD_INSN || insn.getType() == AbstractInsnNode.JUMP_INSN || (insn.getOpcode() >= IRETURN && insn.getOpcode() <= RETURN)) {
			newValue.getUsages().add(insn);
		}
		return newValue;
	}

	@Override
	public UnpickValue binaryOperation(AbstractInsnNode insn, UnpickValue value1, UnpickValue value2) {
		SourceValue sourceValue = delegate.binaryOperation(insn, value1.getSourceValue(), value2.getSourceValue());
		switch (insn.getOpcode()) {
			case IALOAD:
			case FALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case LALOAD:
			case DALOAD:
				return new UnpickValue(sourceValue);
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
				return new UnpickValue(sourceValue, merge(value1, value2));
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
				return new UnpickValue(sourceValue, value1);
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
				return new UnpickValue(sourceValue);
			case PUTFIELD:
				value2.getUsages().add(insn);
				return new UnpickValue(sourceValue);
			default:
				throw new IllegalArgumentException("Unrecognized insn: " + insn.getOpcode());
		}
	}

	@Override
	public UnpickValue ternaryOperation(AbstractInsnNode insn, UnpickValue value1, UnpickValue value2, UnpickValue value3) {
		return new UnpickValue(delegate.ternaryOperation(insn, value1.getSourceValue(), value2.getSourceValue(), value3.getSourceValue()));
	}

	@Override
	public UnpickValue naryOperation(AbstractInsnNode insn, List<? extends UnpickValue> values) {
		SourceValue sourceValue = delegate.naryOperation(insn, values.stream().map(UnpickValue::getSourceValue).collect(Collectors.toList()));
		if (insn.getOpcode() == MULTIANEWARRAY) {
			return new UnpickValue(sourceValue);
		} else {
			boolean hasThis = insn.getOpcode() != INVOKESTATIC && insn.getOpcode() != INVOKEDYNAMIC;
			for (int i = hasThis ? 1 : 0; i < values.size(); i++) {
				values.get(i).getMethodUsages().add(new UnpickValue.MethodUsage(insn, hasThis ? i - 1 : i));
			}
			UnpickValue value = new UnpickValue(sourceValue);
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
		value1.getMethodUsages().addAll(value2.getMethodUsages());
		value1.getUsages().addAll(value2.getUsages());
		value2.setParameterSources(value1.getParameterSources());
		value2.setMethodUsages(value1.getMethodUsages());
		value2.setUsages(value1.getUsages());
		return new UnpickValue(delegate.merge(value1.getSourceValue(), value2.getSourceValue()), value1);
	}
}

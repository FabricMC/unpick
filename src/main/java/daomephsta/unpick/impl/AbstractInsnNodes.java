package daomephsta.unpick.impl;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class AbstractInsnNodes implements Opcodes {
	public static boolean hasLiteralValue(AbstractInsnNode insn) {
		return insn.getOpcode() >= ACONST_NULL && insn.getOpcode() <= LDC;
	}

	@Nullable
	public static Object getLiteralValue(AbstractInsnNode insn) {
		return switch (insn.getOpcode()) {
			case ACONST_NULL -> null;
			case ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 ->
					insn.getOpcode() - ICONST_0; //Neat trick that works because the opcodes are sequential
			case LCONST_0, LCONST_1 -> (long) insn.getOpcode() - LCONST_0;
			case FCONST_0, FCONST_1, FCONST_2 -> (float) insn.getOpcode() - FCONST_0;
			case DCONST_0, DCONST_1 -> (double) insn.getOpcode() - DCONST_0;
			case BIPUSH, SIPUSH -> ((IntInsnNode) insn).operand;
			case LDC -> ((LdcInsnNode) insn).cst;
			default ->
					throw new UnsupportedOperationException("No value retrieval method programmed for " + Utils.visitableToString(insn::accept).trim());
		};
	}

	@Nullable
	public static AbstractInsnNode previousInstruction(AbstractInsnNode insn) {
		while ((insn = insn.getPrevious()) != null) {
			if (insn.getOpcode() >= 0) {
				return insn;
			}
		}

		return null;
	}

	@Nullable
	public static AbstractInsnNode nextInstruction(AbstractInsnNode insn) {
		while ((insn = insn.getNext()) != null) {
			if (insn.getOpcode() >= 0) {
				return insn;
			}
		}

		return null;
	}
}

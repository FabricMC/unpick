package daomephsta.unpick.impl;

import org.jetbrains.annotations.Nullable;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class AbstractInsnNodes implements Opcodes
{
	public static boolean hasLiteralValue(AbstractInsnNode insn)
	{
		return insn.getOpcode() >= ICONST_M1 && insn.getOpcode() <= LDC;
	}

	public static Object getLiteralValue(AbstractInsnNode insn)
	{
		switch (insn.getOpcode())
		{
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
			return insn.getOpcode() - ICONST_0; //Neat trick that works because the opcodes are sequential

		case LCONST_0:
		case LCONST_1:
			return (long) insn.getOpcode() - LCONST_0;

		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
			return (float) insn.getOpcode() - FCONST_0;

		case DCONST_0:
		case DCONST_1:
			return (double) insn.getOpcode() - DCONST_0;

		case BIPUSH:
		case SIPUSH:
			return ((IntInsnNode) insn).operand;

		case LDC:
			return ((LdcInsnNode) insn).cst;

		default :
			throw new UnsupportedOperationException("No value retrieval method programmed for " + Utils.visitableToString(insn::accept).trim());
		}
	}

	public static boolean isLiteral(AbstractInsnNode insn, Object literal)
	{
		Object literalValue = getLiteralValue(insn);
		// Chars are stored as ints, so conversion is necessary
		if (literal instanceof Character && literalValue instanceof Integer)
		{
			Character charLiteral = (char) (int) literalValue;
			return literal.equals(charLiteral);
		}
		// Compare integers by long value, to support widening comparisons
		if (Utils.isIntegral(literalValue) && Utils.isIntegral(literal))
			return ((Number) literalValue).longValue() == ((Number) literal).longValue();
		// Compare floating point numbers by double value, to support widening comparisons
		if (Utils.isFloatingPoint(literalValue) && Utils.isFloatingPoint(literal))
			return ((Number) literalValue).doubleValue() == ((Number) literal).doubleValue();
		return literalValue.equals(literal);
	}

	@Nullable
	public static AbstractInsnNode previousInstruction(AbstractInsnNode insn)
	{
		while ((insn = insn.getPrevious()) != null)
		{
			if (insn.getOpcode() >= 0)
			{
				return insn;
			}
		}

		return null;
	}

	public static AbstractInsnNode nextInstruction(AbstractInsnNode insn)
	{
		while ((insn = insn.getNext()) != null)
		{
			if (insn.getOpcode() >= 0)
			{
				return insn;
			}
		}

		return null;
	}
}

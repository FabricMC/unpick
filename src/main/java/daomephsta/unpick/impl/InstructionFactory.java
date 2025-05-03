package daomephsta.unpick.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class InstructionFactory implements Opcodes {
	public static AbstractInsnNode pushValue(Object value) {
		return switch (value) {
			case Long l -> pushLong(l);
			case Double d -> pushDouble(d);
			case Float f -> pushFloat(f);
			case Number n -> pushInt(n.intValue()); // Shorts and bytes are all ints internally
			case Character c -> pushChar(c);
			case Boolean b -> pushBoolean(b);
			case String s -> pushString(s);
			case Type t -> pushTypeReference(t);
			case null -> new InsnNode(ACONST_NULL);
			default -> throw new UnsupportedOperationException("Pushing reference types is not supported");
		};
	}

	public static AbstractInsnNode pushBoolean(boolean bool) {
		return new InsnNode(bool ? ICONST_1 : ICONST_0);
	}

	public static AbstractInsnNode pushChar(char c) {
		return pushInt(c);
	}

	public static AbstractInsnNode pushInt(int i) {
		if (i >= -1 && i <= 5) {
			return new InsnNode(ICONST_0 + i);
		} else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
			return new IntInsnNode(BIPUSH, i);
		} else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) {
			return new IntInsnNode(SIPUSH, i);
		} else {
			return new LdcInsnNode(i);
		}
	}

	public static AbstractInsnNode pushLong(long l) {
		//Longs seem to be pushed using their consts or LDC, never BIPUSH or SIPUSH
		if (l == 0) {
			return new InsnNode(LCONST_0);
		} else if (l == 1) {
			return new InsnNode(LCONST_1);
		} else {
			return new LdcInsnNode(l);
		}
	}

	public static AbstractInsnNode pushFloat(float f) {
		if (f == 0.0F) {
			return new InsnNode(FCONST_0);
		} else if (f == 1.0F) {
			return new InsnNode(FCONST_1);
		} else if (f == 2.0F) {
			return new InsnNode(FCONST_2);
		} else {
			return new LdcInsnNode(f);
		}
	}

	public static AbstractInsnNode pushDouble(double d) {
		if (d == 0.0D) {
			return new InsnNode(DCONST_0);
		} else if (d == 1.0D) {
			return new InsnNode(DCONST_1);
		} else {
			return new LdcInsnNode(d);
		}
	}

	public static AbstractInsnNode pushString(String s) {
		return new LdcInsnNode(s);
	}

	public static AbstractInsnNode pushTypeReference(Type type) {
		return new LdcInsnNode(type);
	}
}

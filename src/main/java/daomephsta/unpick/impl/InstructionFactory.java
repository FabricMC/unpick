package daomephsta.unpick.impl;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class InstructionFactory implements Opcodes {
	public static AbstractInsnNode pushValue(Object value) {
		if (value instanceof Number) {
			Number number = (Number) value;
			if (number instanceof Long) {
				return pushLong(number.longValue());
			} else if (number instanceof Double) {
				return pushDouble(number.doubleValue());
			} else if (number instanceof Float) {
				return pushFloat(number.floatValue());
			} else { // Shorts and bytes are all ints internally
				return pushInt(number.intValue());
			}
		} else if (value instanceof Character) {
			return pushChar((char) value);
		} else if (value instanceof Boolean) {
			return pushBoolean((boolean) value);
		} else if (value instanceof String) {
			return pushString((String) value);
		} else if (value instanceof Type) {
			return pushTypeReference((Type) value);
		} else {
			throw new UnsupportedOperationException("Pushing reference types is not supported");
		}
	}

	public static void pushValue(MethodVisitor method, Object value) {
		if (value instanceof Number) {
			Number number = (Number) value;
			if (number instanceof Long) {
				pushLong(method, number.longValue());
			} else if (number instanceof Double) {
				pushDouble(method, number.doubleValue());
			} else if (number instanceof Float) {
				pushFloat(method, number.floatValue());
			} else { //Shorts and bytes are all ints internally
				pushInt(method, number.intValue());
			}
		} else if (value instanceof Character) {
			pushChar(method, (char) value);
		} else if (value instanceof Boolean) {
			pushBoolean(method, (boolean) value);
		} else if (value instanceof String) {
			pushString(method, (String) value);
		} else if (value instanceof Type) {
			pushTypeReference(method, (Type) value);
		} else {
			throw new UnsupportedOperationException("Pushing reference types is not supported");
		}
	}

	public static AbstractInsnNode pushBoolean(boolean bool) {
		return new InsnNode(bool ? ICONST_1 : ICONST_0);
	}

	public static void pushBoolean(MethodVisitor method, boolean bool) {
		method.visitInsn(bool ? ICONST_1 : ICONST_0);
	}

	public static AbstractInsnNode pushChar(char c) {
		return pushInt(c);
	}

	public static void pushChar(MethodVisitor method, char c) {
		pushInt(method, c);
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

	public static void pushInt(MethodVisitor method, int i) {
		if (i >= -1 && i <= 5) {
			method.visitInsn(ICONST_0 + i);
		} else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
			method.visitIntInsn(BIPUSH, i);
		} else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) {
			method.visitIntInsn(SIPUSH, i);
		} else {
			method.visitLdcInsn(i);
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

	public static void pushLong(MethodVisitor method, long l) {
		//Longs seem to be pushed using their consts or LDC, never BIPUSH or SIPUSH
		if (l == 0) {
			method.visitInsn(LCONST_0);
		} else if (l == 1) {
			method.visitInsn(LCONST_1);
		} else {
			method.visitLdcInsn(l);
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

	public static void pushFloat(MethodVisitor method, float f) {
		if (f == 0.0F) {
			method.visitInsn(FCONST_0);
		} else if (f == 1.0F) {
			method.visitInsn(FCONST_1);
		} else if (f == 2.0F) {
			method.visitInsn(FCONST_2);
		} else {
			method.visitLdcInsn(f);
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

	public static void pushDouble(MethodVisitor method, double d) {
		if (d == 0.0D) {
			method.visitInsn(DCONST_0);
		} else if (d == 1.0D) {
			method.visitInsn(DCONST_1);
		} else {
			method.visitLdcInsn(d);
		}
	}

	public static AbstractInsnNode pushString(String s) {
		return new LdcInsnNode(s);
	}

	public static void pushString(MethodVisitor method, String s) {
		method.visitLdcInsn(s);
	}

	public static AbstractInsnNode pushTypeReference(Type type) {
		return new LdcInsnNode(type);
	}

	public static void pushTypeReference(MethodVisitor method, Type type) {
		method.visitLdcInsn(type);
	}
}

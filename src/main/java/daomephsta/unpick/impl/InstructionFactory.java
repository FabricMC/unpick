package daomephsta.unpick.impl;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class InstructionFactory
{
	public static AbstractInsnNode pushesValue(Object value)
	{
		if (value instanceof Number)
		{
			Number number = (Number) value;
			if (number instanceof Long)
				return pushesLong(number.longValue());
			else if (number instanceof Double)
				return pushesDouble(number.doubleValue());
			else if (number instanceof Float)
				return pushesFloat(number.floatValue());
			else //Shorts and bytes are all ints internally
				return pushesInt(number.intValue());
		}
		else if (value instanceof Character)
			return pushesChar((char) value);
		else if (value instanceof Boolean)
			return pushesBoolean((boolean) value);
		else if (value instanceof String)
			return pushesString((String) value);
		else if (value instanceof Type)
			return pushesTypeReference((Type) value);
		else
			throw new UnsupportedOperationException("Pushing reference types is not supported");
	}
	
	public static void pushesValue(MethodVisitor method, Object value)
	{
		if (value instanceof Number)
		{
			Number number = (Number) value;
			if (number instanceof Long)
				pushesLong(method, number.longValue());
			else if (number instanceof Double)
				pushesDouble(method, number.doubleValue());
			else if (number instanceof Float)
				pushesFloat(method, number.floatValue());
			else //Shorts and bytes are all ints internally
				pushesInt(method, number.intValue());
		}
		else if (value instanceof Character)
			pushesChar(method, (char) value);
		else if (value instanceof Boolean)
			pushesBoolean(method, (boolean) value);
		else if (value instanceof String)
			pushesString(method, (String) value);
		else if (value instanceof Type)
			pushesTypeReference(method, (Type) value);
		else
			throw new UnsupportedOperationException("Pushing reference types is not supported");
	}
	
	public static AbstractInsnNode pushesBoolean(boolean bool)
	{
		return new InsnNode(bool ? ICONST_1 : ICONST_0);
	}
	
	public static void pushesBoolean(MethodVisitor method, boolean bool)
	{
		method.visitInsn(bool ? ICONST_1 : ICONST_0);
	}
	
	public static AbstractInsnNode pushesChar(char c)
	{
		return pushesInt(c);
	}
	
	public static void pushesChar(MethodVisitor method, char c)
	{
		pushesInt(method, c);
	}
	
	private static final int[] I_OPCODES = {ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5}; 
	public static AbstractInsnNode pushesInt(int i)
	{
		if (i == -1) 
			return new InsnNode(ICONST_M1);
		else if(i >= 0 && i < I_OPCODES.length)
			return new InsnNode(I_OPCODES[i]);
		else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE)
			return new IntInsnNode(BIPUSH, i);
		else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE)
			return new IntInsnNode(SIPUSH, i);
		else
			return new LdcInsnNode(i);
	}
	
	public static void pushesInt(MethodVisitor method, int i)
	{
		if (i == -1) 
			method.visitInsn(ICONST_M1);
		else if(i >= 0 && i < I_OPCODES.length)
			method.visitInsn(I_OPCODES[i]);
		else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE)
			method.visitIntInsn(BIPUSH, i);
		else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE)
			method.visitIntInsn(SIPUSH, i);
		else
			method.visitLdcInsn(i);
	}
	
	public static AbstractInsnNode pushesLong(long l)
	{
		//Longs seem to be pushed using their consts or LDC, never BIPUSH or SIPUSH
		if (l == 0)
			return new InsnNode(LCONST_0);
		else if (l == 1)
			return new InsnNode(LCONST_1);
		else
			return new LdcInsnNode(l);
	}
	
	public static void pushesLong(MethodVisitor method, long l)
	{
		//Longs seem to be pushed using their consts or LDC, never BIPUSH or SIPUSH
		if (l == 0)
			method.visitInsn(LCONST_0);
		else if (l == 1)
			method.visitInsn(LCONST_1); 
		else
			method.visitLdcInsn(l);
	}
	
	public static AbstractInsnNode pushesFloat(float f)
	{
		if (f == 0.0F)
			return new InsnNode(FCONST_0);
		else if (f == 1.0F) 
			return new InsnNode(FCONST_1);
		else if (f == 2.0F)
			return new InsnNode(FCONST_2);
		else
			return new LdcInsnNode(f);
	}
	
	public static void pushesFloat(MethodVisitor method, float f)
	{
		if (f == 0.0F)
			method.visitInsn(FCONST_0);
		else if (f == 1.0F) 
			method.visitInsn(FCONST_1);
		else if (f == 2.0F)
			method.visitInsn(FCONST_2);
		else
			method.visitLdcInsn(f);
	}
	
	public static AbstractInsnNode pushesDouble(double d)
	{
		if (d == 0.0D)
			return new InsnNode(DCONST_0);
		else if (d == 1.0D) 
			return new InsnNode(DCONST_1);
		else
			return new LdcInsnNode(d);
	}
	
	public static void pushesDouble(MethodVisitor method, double d)
	{
		if (d == 0.0D)
			method.visitInsn(DCONST_0);
		else if (d == 1.0D) 
			method.visitInsn(DCONST_1);
		else
			method.visitLdcInsn(d);
	}
	
	public static AbstractInsnNode pushesString(String s)
	{
		return new LdcInsnNode(s);
	}
	
	public static void pushesString(MethodVisitor method, String s)
	{
		method.visitLdcInsn(s);
	}
	
	public static AbstractInsnNode pushesTypeReference(Type type)
	{
		return new LdcInsnNode(type);
	}
	
	public static void pushesTypeReference(MethodVisitor method, Type type)
	{
		method.visitLdcInsn(type);
	}
}

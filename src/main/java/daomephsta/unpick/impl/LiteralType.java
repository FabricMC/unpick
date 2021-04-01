package daomephsta.unpick.impl;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

public enum LiteralType
{
	BYTE(Byte.class, byte.class, Type.BYTE_TYPE, Opcodes.IRETURN)
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(Object literal)
		{ return InstructionFactory.pushesInt(((Number) literal).byteValue()); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, Object literal)
		{ InstructionFactory.pushesInt(mv, ((Number) literal).byteValue()); }

		@Override
		public Object parse(String valueString)
		{ return Byte.parseByte(valueString); }
	},
	INT(Integer.class, int.class, Type.INT_TYPE, Opcodes.IRETURN)
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(Object literal)
			{ return InstructionFactory.pushesInt(((Number) literal).intValue()); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, Object literal)
			{ InstructionFactory.pushesInt(mv, ((Number) literal).intValue()); }
		
		@Override
		public Object parse(String valueString)
			{ return Integer.parseInt(valueString); }
	},
	LONG(Long.class, long.class, Type.LONG_TYPE, Opcodes.LRETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(Object literal)
			{ return InstructionFactory.pushesLong(((Number) literal).longValue()); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, Object literal)
			{ InstructionFactory.pushesLong(mv, ((Number) literal).longValue()); }
		
		@Override
		public Object parse(String valueString)
			{ return Long.parseLong(valueString); }
	},
	FLOAT(Float.class, float.class, Type.FLOAT_TYPE, Opcodes.FRETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(Object literal)
			{ return InstructionFactory.pushesFloat(((Number) literal).floatValue()); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, Object literal)
			{ InstructionFactory.pushesFloat(mv, ((Number) literal).floatValue()); }
		
		@Override
		public Object parse(String valueString)
			{ return Float.parseFloat(valueString); }
	},
	DOUBLE(Double.class, double.class, Type.DOUBLE_TYPE, Opcodes.DRETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(Object literal)
			{ return InstructionFactory.pushesDouble(((Number) literal).doubleValue()); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, Object literal)
			{ InstructionFactory.pushesDouble(mv, ((Number) literal).doubleValue()); }
		
		@Override
		public Object parse(String valueString)
			{ return Double.parseDouble(valueString); }
	},
	STRING(String.class, String.class, Type.getType(String.class), Opcodes.ARETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(Object literal)
			{ return InstructionFactory.pushesString((String) literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, Object literal)
			{ InstructionFactory.pushesString(mv, (String) literal); }
		
		@Override
		public Object parse(String valueString)
			{ return valueString; }
	},
	TYPE_REFERENCE(Type.class, Type.class, Type.getType(Type.class), Opcodes.ARETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(Object literal)
			{ return InstructionFactory.pushesTypeReference((Type) literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, Object literal)
			{ InstructionFactory.pushesTypeReference(mv, (Type) literal); }
		
		@Override
		public Object parse(String valueString)
			{ return Type.getType(valueString); }
	};
	
	private static final Map<Class<?>, LiteralType> valuesByClass = new HashMap<>();
	private static final Map<Type, LiteralType> valuesByType = new HashMap<>();
	static
	{
		for (LiteralType literalType : values())
		{
			valuesByClass.put(literalType.getPrimitiveClass(), literalType);
			valuesByClass.put(literalType.getBoxClass(), literalType);
			valuesByType.put(literalType.getType(), literalType);
		}
	}
	
	private final Class<?> boxed, primitive;
	private final Type type;
	private final int returnOpcode;
	
	private LiteralType(Class<?> boxed, Class<?> primitive, Type type, int returnOpcode)
	{
		this.boxed = boxed;
		this.primitive = primitive;
		this.type = type;
		this.returnOpcode = returnOpcode;
	}
	
	public static LiteralType from(Class<?> clazz)
	{
		if (valuesByClass.containsKey(clazz))
			return valuesByClass.get(clazz);
		else
			throw new IllegalArgumentException(clazz + " is not an byte, int, long, float, double, String, or type reference");
	}
	
	public static LiteralType from(Type type)
	{
		if (valuesByType.containsKey(type))
			return valuesByType.get(type);
		else 
			throw new IllegalArgumentException(type + " is not an byte, int, float, long, double, String, or type reference");
	}
	
	public AbstractInsnNode createReturnInsn()
	{
		return new InsnNode(getReturnOpcode());
	}
	
	public void appendReturnInsn(MethodVisitor mv)
	{
		mv.visitInsn(getReturnOpcode());
	}
	
	public int getReturnOpcode()
	{
		return returnOpcode;
	}

	public abstract AbstractInsnNode createLiteralPushInsn(Object literal);
	
	public abstract void appendLiteralPushInsn(MethodVisitor mv, Object literal);

	public abstract Object parse(String valueString);
	
	public Type getType()
	{
		return type;
	}
	
	public String getTypeDescriptor()
	{
		return type.getDescriptor();
	}
	
	public Class<?> getBoxClass()
	{
		return boxed;
	}
	
	public Class<?> getPrimitiveClass()
	{
		return primitive;
	}
}

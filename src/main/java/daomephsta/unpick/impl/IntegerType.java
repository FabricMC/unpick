package daomephsta.unpick.impl;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

public enum IntegerType
{
	BYTE(Byte.class, byte.class, Type.BYTE_TYPE, Opcodes.IAND, Opcodes.IRETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(long literal)
			{ return InstructionFactory.pushesInt((byte) literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, long literal)
			{ InstructionFactory.pushesInt(mv, (byte) literal); }
		
		@Override
		public Number box(long value)
			{ return Byte.valueOf((byte) value); }

		@Override
		public Number binaryNegate(Number value)
			{ return ~value.intValue(); }

		@Override
		public long toUnsignedLong(Number value)
			{ return Integer.toUnsignedLong(value.intValue()); }
	},
	SHORT(Short.class, short.class, Type.SHORT_TYPE, Opcodes.IAND, Opcodes.IRETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(long literal)
			{ return InstructionFactory.pushesInt((short) literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, long literal)
			{ InstructionFactory.pushesInt(mv, (short) literal); }
		
		@Override
		public Number box(long value)
			{ return Short.valueOf((short) value); }

		@Override
		public Number binaryNegate(Number value)
			{ return ~value.intValue(); }

		@Override
		public long toUnsignedLong(Number value)
			{ return Integer.toUnsignedLong(value.intValue()); }
	},
	INT(Integer.class, int.class, Type.INT_TYPE, Opcodes.IAND, Opcodes.IRETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(long literal)
			{ return InstructionFactory.pushesInt((int) literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, long literal)
			{ InstructionFactory.pushesInt(mv, (int) literal); }
		
		@Override
		public Number box(long value)
			{ return new Integer((int) value); }

		@Override
		public Number binaryNegate(Number value)
			{ return ~value.intValue(); }

		@Override
		public long toUnsignedLong(Number value)
			{ return Integer.toUnsignedLong(value.intValue()); }
	},
	LONG(Long.class, long.class, Type.LONG_TYPE, Opcodes.LAND, Opcodes.LRETURN) 
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(long literal)
			{ return InstructionFactory.pushesLong(literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, long literal)
			{ InstructionFactory.pushesLong(mv, literal); }
		
		@Override
		public Number box(long value)
			{ return new Long(value); }

		@Override
		public Number binaryNegate(Number value)
			{ return ~value.longValue(); }

		@Override
		public long toUnsignedLong(Number value)
			{ return value.longValue(); }
	};
	
	private final Class<? extends Number> boxed, primitive;
	private final Type type;
	private final int logicalOpcodesStart, returnOpcode;
	
	private IntegerType(Class<? extends Number> boxed, Class<? extends Number> primitive, Type type, int logicalOpcodesStart, int returnOpcode)
	{
		this.boxed = boxed;
		this.primitive = primitive;
		this.type = type;
		this.logicalOpcodesStart = logicalOpcodesStart;
		this.returnOpcode = returnOpcode;
	}
	
	public static IntegerType from(Class<?> clazz)
	{ 
		for (IntegerType type : values())
		{
			if (clazz == type.getBoxClass() || clazz == type.getPrimitiveClass())
				return type;
		}
		throw new IllegalArgumentException(clazz + " is not one of: " + describeValidTypes());
	}

	private static String describeValidTypes()
	{
		return Arrays.stream(values())
			.map(t -> t.name().toLowerCase(Locale.ROOT))
			.collect(Collectors.joining(", "));
	}
	
	public AbstractInsnNode createAndInsn()
	{
		return new InsnNode(getAndOpcode());
	}
	
	public void appendAndInsn(MethodVisitor mv)
	{
		mv.visitInsn(getAndOpcode());
	}

	public int getAndOpcode()
	{
		return logicalOpcodesStart + 0;
	}
	
	public AbstractInsnNode createOrInsn()
	{
		return new InsnNode(getOrOpcode());
	}
	
	public void appendOrInsn(MethodVisitor mv)
	{
		mv.visitInsn(getOrOpcode());
	}

	public int getOrOpcode()
	{
		return logicalOpcodesStart + 2;
	}
	
	public AbstractInsnNode createXorInsn()
	{
		return new InsnNode(getXorOpcode());
	}
	
	public void appendXorInsn(MethodVisitor mv)
	{
		mv.visitInsn(getXorOpcode());
	}

	public int getXorOpcode()
	{
		return logicalOpcodesStart + 4;
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

	public abstract AbstractInsnNode createLiteralPushInsn(long literal);
	
	public abstract void appendLiteralPushInsn(MethodVisitor mv, long literal);
	
	public String getTypeDescriptor()
	{
		return type.getDescriptor();
	}
	
	public Class<? extends Number> getBoxClass()
	{
		return boxed;
	}
	
	public Class<? extends Number> getPrimitiveClass()
	{
		return primitive;
	}

	public abstract Number box(long value);
	
	public abstract Number binaryNegate(Number value);

	public abstract long toUnsignedLong(Number value);
}

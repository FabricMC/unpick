package daomephsta.unpick.impl;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum IntegerType
{
	BYTE(Byte.class, byte.class, Type.BYTE_TYPE, Opcodes.IAND, Opcodes.IRETURN)
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(long literal)
			{ return InstructionFactory.pushInt((byte) literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, long literal)
			{ InstructionFactory.pushInt(mv, (byte) literal); }

		@Override
		public Number box(long value)
			{ return Byte.valueOf((byte) value); }

		@Override
		public Number binaryNegate(Number value)
			{ return (byte) ~value.byteValue(); }

		@Override
		public long toUnsignedLong(Number value)
			{ return Byte.toUnsignedLong(value.byteValue()); }

		@Override
		public Number parse(String valueString)
			{ return Byte.parseByte(valueString); }
	},
	SHORT(Short.class, short.class, Type.SHORT_TYPE, Opcodes.IAND, Opcodes.IRETURN)
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(long literal)
			{ return InstructionFactory.pushInt((short) literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, long literal)
			{ InstructionFactory.pushInt(mv, (short) literal); }

		@Override
		public Number box(long value)
			{ return Short.valueOf((short) value); }

		@Override
		public Number binaryNegate(Number value)
			{ return (short) ~value.shortValue(); }

		@Override
		public long toUnsignedLong(Number value)
			{ return Short.toUnsignedLong(value.shortValue()); }

		@Override
		public Number parse(String valueString)
			{ return Short.parseShort(valueString); }
	},
	INT(Integer.class, int.class, Type.INT_TYPE, Opcodes.IAND, Opcodes.IRETURN)
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(long literal)
			{ return InstructionFactory.pushInt((int) literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, long literal)
			{ InstructionFactory.pushInt(mv, (int) literal); }

		@Override
		public Number box(long value)
			{ return Integer.valueOf((int) value); }

		@Override
		public Number binaryNegate(Number value)
			{ return ~value.intValue(); }

		@Override
		public long toUnsignedLong(Number value)
			{ return Integer.toUnsignedLong(value.intValue()); }

		@Override
		public Number parse(String valueString)
			{ return Integer.parseInt(valueString); }
	},
	LONG(Long.class, long.class, Type.LONG_TYPE, Opcodes.LAND, Opcodes.LRETURN)
	{
		@Override
		public AbstractInsnNode createLiteralPushInsn(long literal)
			{ return InstructionFactory.pushLong(literal); }

		@Override
		public void appendLiteralPushInsn(MethodVisitor mv, long literal)
			{ InstructionFactory.pushLong(mv, literal); }

		@Override
		public Number box(long value)
			{ return Long.valueOf(value); }

		@Override
		public Number binaryNegate(Number value)
			{ return ~value.longValue(); }

		@Override
		public long toUnsignedLong(Number value)
			{ return value.longValue(); }

		@Override
		public Number parse(String valueString)
			{ return Long.parseLong(valueString); }
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

	public static IntegerType from(Type type)
	{
		for (IntegerType intType : values())
		{
			if (intType.type == type)
				return intType;
		}
		throw new IllegalArgumentException(type + " is not one of: " + describeValidTypes());
	}

	public static IntegerType from(Object literal)
	{
		for (IntegerType type : values())
		{
			if (literal.getClass() == type.getBoxClass() || literal.getClass() == type.getPrimitiveClass())
				return type;
		}
		throw new IllegalArgumentException(literal + " is not one of: " + describeValidTypes());
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

	public abstract Number parse(String valueString);
}

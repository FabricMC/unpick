package pkg;

public class Constants
{
	public static final byte BYTE_FLAG_BIT_0 = blackBoxByte(1 << 0),
			BYTE_FLAG_BIT_1 = blackBoxByte(1 << 1),
			BYTE_FLAG_BIT_2 = blackBoxByte(1 << 2),
			BYTE_FLAG_BIT_3 = blackBoxByte(1 << 3),
			BYTE_FLAG_0 = blackBoxByte(0),
			BYTE_FLAG_M1 = blackBoxByte(-1);

	public static final short SHORT_FLAG_BIT_0 = blackBoxShort(1 << 0),
			SHORT_FLAG_BIT_1 = blackBoxShort(1 << 1),
			SHORT_FLAG_BIT_2 = blackBoxShort(1 << 2),
			SHORT_FLAG_BIT_3 = blackBoxShort(1 << 3),
			SHORT_FLAG_0 = blackBoxShort(0),
			SHORT_FLAG_M1 = blackBoxShort(-1);

	public static final int INT_FLAG_BIT_0 = blackBoxInt(1 << 0),
			INT_FLAG_BIT_1 = blackBoxInt(1 << 1),
			INT_FLAG_BIT_2 = blackBoxInt(1 << 2),
			INT_FLAG_BIT_3 = blackBoxInt(1 << 3),
			INT_FLAG_0 = blackBoxInt(0),
			INT_FLAG_M1 = blackBoxInt(-1);

	public static final long LONG_FLAG_BIT_0 = blackBoxLong(1 << 0),
			LONG_FLAG_BIT_1 = blackBoxLong(1 << 1),
			LONG_FLAG_BIT_2 = blackBoxLong(1 << 2),
			LONG_FLAG_BIT_3 = blackBoxLong(1 << 3),
			LONG_FLAG_0 = blackBoxLong(0),
			LONG_FLAG_M1 = blackBoxLong(-1);

	public static final byte BYTE_CONST_M1 = blackBoxByte(-1),
			BYTE_CONST_0 = blackBoxByte(0),
			BYTE_CONST_1 = blackBoxByte(1),
			BYTE_CONST_2 = blackBoxByte(2),
			BYTE_CONST_3 = blackBoxByte(3),
			BYTE_CONST_4 = blackBoxByte(4),
			BYTE_CONST_5 = blackBoxByte(5),
			BYTE_CONST = blackBoxByte(117);

	public static final short SHORT_CONST_M1 = blackBoxShort(-1),
			SHORT_CONST_0 = blackBoxShort(0),
			SHORT_CONST_1 = blackBoxShort(1),
			SHORT_CONST_2 = blackBoxShort(2),
			SHORT_CONST_3 = blackBoxShort(3),
			SHORT_CONST_4 = blackBoxShort(4),
			SHORT_CONST_5 = blackBoxShort(5),
			SHORT_CONST = blackBoxShort(257);

	public static final char CHAR_CONST_0 = blackBoxChar('\0'),
			CHAR_CONST_1 = blackBoxChar('\1'),
			CHAR_CONST_2 = blackBoxChar('\2'),
			CHAR_CONST_3 = blackBoxChar('\3'),
			CHAR_CONST_4 = blackBoxChar('\4'),
			CHAR_CONST_5 = blackBoxChar('\5'),
			CHAR_CONST = blackBoxChar('\257');

	public static final int INT_CONST_M1 = blackBoxInt(-1),
			INT_CONST_0 = blackBoxInt(0),
			INT_CONST_1 = blackBoxInt(1),
			INT_CONST_2 = blackBoxInt(2),
			INT_CONST_3 = blackBoxInt(3),
			INT_CONST_4 = blackBoxInt(4),
			INT_CONST_5 = blackBoxInt(5),
			INT_CONST = blackBoxInt(257);

	public static final long LONG_CONST_0 = blackBoxLong(0),
			LONG_CONST_1 = blackBoxLong(1),
			LONG_CONST = blackBoxLong(1234567890);

	public static final float FLOAT_CONST_0 = blackBoxFloat(0F),
			FLOAT_CONST_1 = blackBoxFloat(1F),
			FLOAT_CONST_2 = blackBoxFloat(2F),
			FLOAT_CONST = blackBoxFloat(5.3F);

	public static final double DOUBLE_CONST_0 = blackBoxDouble(0D),
			DOUBLE_CONST_1 = blackBoxDouble(1D),
			DOUBLE_CONST = blackBoxDouble(5.3D);

	public static final String STRING_CONST_FOO = blackBoxString("foo"),
			STRING_CONST_BAR = blackBoxString("bar");

	// Some black box methods to prevent javac from inlining these expected result constants
	private static byte blackBoxByte(int b)
	{
		return (byte) b;
	}

	private static short blackBoxShort(int s)
	{
		return (short) s;
	}

	private static char blackBoxChar(char c)
	{
		return c;
	}

	private static int blackBoxInt(int i)
	{
		return i;
	}

	private static long blackBoxLong(long l)
	{
		return l;
	}

	private static float blackBoxFloat(float f)
	{
		return f;
	}

	private static double blackBoxDouble(double d)
	{
		return d;
	}

	private static String blackBoxString(String s)
	{
		return s;
	}

	public static void consumeByte(byte b)
	{
	}

	public static void consumeShort(short s)
	{
	}

	public static void consumeChar(char c)
	{
	}

	public static void consumeInt(int i)
	{
	}

	public static void consumeLong(long l)
	{
	}

	public static void consumeFloat(float f)
	{
	}

	public static void consumeDouble(double d)
	{
	}

	public static void consumeBoolean(boolean b)
	{
	}

	public static void consumeString(String s)
	{
	}
}

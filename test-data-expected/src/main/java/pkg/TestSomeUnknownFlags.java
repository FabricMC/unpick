package pkg;

public class TestSomeUnknownFlags
{
	public void testByte()
	{
		Constants.consumeByte((byte) (Constants.BYTE_FLAG_BIT_0 | 0b10000));
	}

	public void testShort()
	{
		Constants.consumeShort((short) (Constants.SHORT_FLAG_BIT_0 | 0b10000));
	}

	public void testInt()
	{
		Constants.consumeInt(Constants.INT_FLAG_BIT_0 | 0b10000);
	}

	public void testLong()
	{
		Constants.consumeLong(Constants.LONG_FLAG_BIT_0 | 0b10000);
	}
}

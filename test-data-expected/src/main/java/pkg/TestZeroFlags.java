package pkg;

public class TestZeroFlags
{
	public void testByte()
	{
		Constants.consumeByte(Constants.BYTE_FLAG_0);
	}

	public void testShort()
	{
		Constants.consumeShort(Constants.SHORT_FLAG_0);
	}

	public void testInt()
	{
		Constants.consumeInt(Constants.INT_FLAG_0);
	}

	public void testLong()
	{
		Constants.consumeLong(Constants.LONG_FLAG_0);
	}
}

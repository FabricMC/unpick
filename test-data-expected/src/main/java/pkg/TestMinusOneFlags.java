package pkg;

public class TestMinusOneFlags
{
	public void testByte()
	{
		Constants.consumeByte(Constants.BYTE_FLAG_M1);
	}

	public void testShort()
	{
		Constants.consumeShort(Constants.SHORT_FLAG_M1);
	}

	public void testInt()
	{
		Constants.consumeInt(Constants.INT_FLAG_M1);
	}

	public void testLong()
	{
		Constants.consumeLong(Constants.LONG_FLAG_M1);
	}
}

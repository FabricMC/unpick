package pkg;

public class TestNegatedByteFlagsReturn
{
	public byte test1()
	{
		return (byte) ~Constants.BYTE_FLAG_BIT_2;
	}

	public byte test2()
	{
		return (byte) ~(Constants.BYTE_FLAG_BIT_2 | Constants.BYTE_FLAG_BIT_3);
	}

	public byte test3()
	{
		return (byte) ~(Constants.BYTE_FLAG_BIT_1 | Constants.BYTE_FLAG_BIT_3);
	}

	public byte test4()
	{
		return (byte) ~(Constants.BYTE_FLAG_BIT_0 | Constants.BYTE_FLAG_BIT_1 | Constants.BYTE_FLAG_BIT_2);
	}
}

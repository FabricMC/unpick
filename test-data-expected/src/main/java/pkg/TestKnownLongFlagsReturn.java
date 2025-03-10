package pkg;

public class TestKnownLongFlagsReturn
{
	public long test1()
	{
		return Constants.LONG_FLAG_BIT_2;
	}

	public long test2()
	{
		return Constants.LONG_FLAG_BIT_2 | Constants.LONG_FLAG_BIT_3;
	}

	public long test3()
	{
		return Constants.LONG_FLAG_BIT_1 | Constants.LONG_FLAG_BIT_3;
	}

	public long test4()
	{
		return Constants.LONG_FLAG_BIT_0 | Constants.LONG_FLAG_BIT_1 | Constants.LONG_FLAG_BIT_2;
	}
}

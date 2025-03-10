package pkg;

public class TestKnownIntFlagsReturn
{
	public int test1()
	{
		return Constants.INT_FLAG_BIT_2;
	}

	public int test2()
	{
		return Constants.INT_FLAG_BIT_2 | Constants.INT_FLAG_BIT_3;
	}

	public int test3()
	{
		return Constants.INT_FLAG_BIT_1 | Constants.INT_FLAG_BIT_3;
	}

	public int test4()
	{
		return Constants.INT_FLAG_BIT_0 | Constants.INT_FLAG_BIT_1 | Constants.INT_FLAG_BIT_2;
	}
}

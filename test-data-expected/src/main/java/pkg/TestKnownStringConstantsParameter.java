package pkg;

public class TestKnownStringConstantsParameter
{
	public void test1()
	{
		Constants.consumeString(Constants.STRING_CONST_FOO);
	}

	public void test2()
	{
		Constants.consumeString(Constants.STRING_CONST_BAR);
	}

	public void test3()
	{
		Constants.consumeString(Constants.STRING_CONST_NULL);
	}
}

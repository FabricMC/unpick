package pkg;

public class TestKnownClassConstantsReturn
{
	public Class<?> test1()
	{
		return Constants.CLASS_CONST_STRING;
	}

	public Class<?> test2()
	{
		return Constants.CLASS_CONST_INTEGER;
	}

	public Class<?> test3()
	{
		return Constants.CLASS_CONST_NULL;
	}
}

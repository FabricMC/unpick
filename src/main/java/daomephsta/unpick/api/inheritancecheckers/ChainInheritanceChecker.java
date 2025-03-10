package daomephsta.unpick.api.inheritancecheckers;

import org.jetbrains.annotations.Nullable;

public class ChainInheritanceChecker implements IInheritanceChecker
{
	private final IInheritanceChecker[] checkers;

	public ChainInheritanceChecker(IInheritanceChecker[] checkers)
	{
		this.checkers = checkers;
	}

	@Override
	@Nullable
	public ClassInfo getClassInfo(String className)
	{
		for (IInheritanceChecker checker : checkers)
		{
			ClassInfo classInfo = checker.getClassInfo(className);
			if (classInfo != null)
			{
				return classInfo;
			}
		}

		return null;
	}
}

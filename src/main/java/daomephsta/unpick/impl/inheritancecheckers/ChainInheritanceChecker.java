package daomephsta.unpick.impl.inheritancecheckers;

import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.impl.Utils;
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

	@Override
	public IInheritanceChecker chain(IInheritanceChecker... others)
	{
		return new ChainInheritanceChecker(Utils.concat(checkers, others));
	}
}

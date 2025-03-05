package daomephsta.unpick.api.inheritancecheckers;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.impl.inheritancecheckers.BytecodeAnalysisInheritanceChecker;

public final class InheritanceCheckers
{
	public static IInheritanceChecker bytecodeAnalysis(IClassResolver classResolver)
	{
		return new BytecodeAnalysisInheritanceChecker(classResolver);
	}
}

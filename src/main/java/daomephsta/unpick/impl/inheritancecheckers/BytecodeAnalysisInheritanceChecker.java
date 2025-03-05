package daomephsta.unpick.impl.inheritancecheckers;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.inheritancecheckers.IInheritanceChecker;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class BytecodeAnalysisInheritanceChecker implements IInheritanceChecker
{
	private final IClassResolver classResolver;

	public BytecodeAnalysisInheritanceChecker(IClassResolver classResolver)
	{
		this.classResolver = classResolver;
	}

	@Override
	@Nullable
	public ClassInfo getClassInfo(String className)
	{
		ClassReader reader;
		try
		{
			reader = classResolver.resolveClass(className);
		}
		catch (IClassResolver.ClassResolutionException e)
		{
			return null;
		}

		return new ClassInfo(reader.getSuperName(), reader.getInterfaces(), (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0);
	}
}

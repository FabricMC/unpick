package daomephsta.unpick.impl.inheritancecheckers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;

import org.jetbrains.annotations.Nullable;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BytecodeAnalysisInheritanceChecker implements IInheritanceChecker
{
	private final IClassResolver classResolver;
	private final ConcurrentMap<String, ClassInfo> classInfoCache = new ConcurrentHashMap<>();

	public BytecodeAnalysisInheritanceChecker(IClassResolver classResolver)
	{
		this.classResolver = classResolver;
	}

	@Override
	@Nullable
	public ClassInfo getClassInfo(String className)
	{
		return classInfoCache.computeIfAbsent(className, name ->
		{
			ClassReader reader = classResolver.resolveClass(name);
			if (reader == null)
				return null;

			return new ClassInfo(reader.getSuperName(), reader.getInterfaces(), (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0);
		});
	}
}

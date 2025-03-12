package daomephsta.unpick.impl.classresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.impl.constantresolvers.ClasspathConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.ClasspathInheritanceChecker;

import org.jetbrains.annotations.Nullable;

import org.objectweb.asm.ClassReader;

import java.io.IOException;

public class ClasspathClassResolver implements IClassResolver
{
	@Override
	@Nullable
	public ClassReader resolveClass(String internalName)
	{
		try
		{
			return new ClassReader(internalName);
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	public IConstantResolver asConstantResolver()
	{
		return new ClasspathConstantResolver();
	}

	@Override
	public IInheritanceChecker asInheritanceChecker()
	{
		return new ClasspathInheritanceChecker();
	}
}

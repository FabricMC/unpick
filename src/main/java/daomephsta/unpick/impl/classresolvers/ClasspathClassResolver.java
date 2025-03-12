package daomephsta.unpick.impl.classresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.impl.constantresolvers.ClasspathConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.ClasspathInheritanceChecker;

import org.objectweb.asm.ClassReader;

import java.io.IOException;

public class ClasspathClassResolver implements IClassResolver
{
	@Override
	public ClassReader resolveClass(String internalName) throws ClassResolutionException
	{
		try
		{
			return new ClassReader(internalName);
		}
		catch (IOException e)
		{
			throw new ClassResolutionException(e);
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

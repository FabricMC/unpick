package daomephsta.unpick.impl.classresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.constantresolvers.ChainConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.ChainInheritanceChecker;

import org.objectweb.asm.ClassReader;

import java.util.ArrayList;
import java.util.List;

public class ChainClassResolver implements IClassResolver
{
	private final IClassResolver[] resolvers;

	public ChainClassResolver(IClassResolver... resolvers)
	{
		this.resolvers = resolvers;
	}

	@Override
	public ClassReader resolveClass(String internalName) throws ClassResolutionException
	{
		List<ClassResolutionException> exceptions = new ArrayList<>();
		for (IClassResolver resolver : resolvers)
		{
			try
			{
				return resolver.resolveClass(internalName);
			}
			catch (ClassResolutionException e)
			{
				exceptions.add(e);
			}
		}

		if (exceptions.isEmpty())
		{
			throw new ClassResolutionException("No resolvers");
		}
		else
		{
			ClassResolutionException exception = exceptions.get(exceptions.size() - 1);
			for (int i = 0; i < exceptions.size() - 1; i++)
			{
				exception.addSuppressed(exceptions.get(i));
			}
			throw exception;
		}
	}

	@Override
	public IConstantResolver asConstantResolver()
	{
		IConstantResolver[] constantResolvers = new IConstantResolver[resolvers.length];
		for (int i = 0; i < resolvers.length; i++)
		{
			constantResolvers[i] = resolvers[i].asConstantResolver();
		}
		return new ChainConstantResolver(constantResolvers);
	}

	@Override
	public IInheritanceChecker asInheritanceChecker()
	{
		IInheritanceChecker[] inheritanceCheckers = new IInheritanceChecker[resolvers.length];
		for (int i = 0; i < resolvers.length; i++)
		{
			inheritanceCheckers[i] = resolvers[i].asInheritanceChecker();
		}
		return new ChainInheritanceChecker(inheritanceCheckers);
	}

	@Override
	public IClassResolver chain(IClassResolver... others)
	{
		return new ChainClassResolver(Utils.concat(resolvers, others));
	}
}

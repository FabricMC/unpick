package daomephsta.unpick.impl.classresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.constantresolvers.ChainConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.ChainInheritanceChecker;

import org.jetbrains.annotations.Nullable;

import org.objectweb.asm.ClassReader;

public class ChainClassResolver implements IClassResolver
{
	private final IClassResolver[] resolvers;

	public ChainClassResolver(IClassResolver... resolvers)
	{
		this.resolvers = resolvers;
	}

	@Override
	@Nullable
	public ClassReader resolveClass(String internalName)
	{
		for (IClassResolver resolver : resolvers)
		{
			ClassReader cr = resolver.resolveClass(internalName);
			if (cr != null)
			{
				return cr;
			}
		}

		return null;
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

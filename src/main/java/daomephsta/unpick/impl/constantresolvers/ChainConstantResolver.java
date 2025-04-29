package daomephsta.unpick.impl.constantresolvers;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.impl.Utils;

public class ChainConstantResolver implements IConstantResolver {
	private final IConstantResolver[] constantResolvers;

	public ChainConstantResolver(IConstantResolver[] constantResolvers) {
		this.constantResolvers = constantResolvers;
	}

	@Override
	@Nullable
	public ResolvedConstant resolveConstant(String owner, String name) {
		for (IConstantResolver constantResolver : constantResolvers) {
			ResolvedConstant resolvedConstant = constantResolver.resolveConstant(owner, name);
			if (resolvedConstant != null) {
				return resolvedConstant;
			}
		}

		return null;
	}

	@Override
	public IConstantResolver chain(IConstantResolver... others) {
		return new ChainConstantResolver(Utils.concat(constantResolvers, others));
	}
}

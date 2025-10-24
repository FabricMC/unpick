package daomephsta.unpick.impl.classresolvers;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.api.classresolvers.IMemberChecker;
import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.constantresolvers.ChainConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.ChainInheritanceChecker;
import daomephsta.unpick.impl.membercheckers.ChainMemberChecker;

public class ChainClassResolver implements IClassResolver {
	private final IClassResolver[] resolvers;

	public ChainClassResolver(IClassResolver... resolvers) {
		this.resolvers = resolvers;
	}

	@Override
	@Nullable
	public ClassReader resolveClass(String internalName) {
		for (IClassResolver resolver : resolvers) {
			ClassReader cr = resolver.resolveClass(internalName);
			if (cr != null) {
				return cr;
			}
		}

		return null;
	}

	@Override
	public @Nullable ClassNode resolveClassNode(String internalName, int readerFlags) {
		for (IClassResolver resolver : resolvers) {
			ClassNode classNode = resolver.resolveClassNode(internalName, readerFlags);
			if (classNode != null) {
				return classNode;
			}
		}
		return null;
	}

	@Override
	public IConstantResolver asConstantResolver() {
		IConstantResolver[] constantResolvers = new IConstantResolver[resolvers.length];
		for (int i = 0; i < resolvers.length; i++) {
			constantResolvers[i] = resolvers[i].asConstantResolver();
		}
		return new ChainConstantResolver(constantResolvers);
	}

	@Override
	public IInheritanceChecker asInheritanceChecker() {
		IInheritanceChecker[] inheritanceCheckers = new IInheritanceChecker[resolvers.length];
		for (int i = 0; i < resolvers.length; i++) {
			inheritanceCheckers[i] = resolvers[i].asInheritanceChecker();
		}
		return new ChainInheritanceChecker(inheritanceCheckers);
	}

	@Override
	public IMemberChecker asMemberChecker() {
		IMemberChecker[] memberCheckers = new IMemberChecker[resolvers.length];
		for (int i = 0; i < resolvers.length; i++) {
			memberCheckers[i] = resolvers[i].asMemberChecker();
		}
		return new ChainMemberChecker(memberCheckers);
	}

	@Override
	public IClassResolver chain(IClassResolver... others) {
		return new ChainClassResolver(Utils.concat(resolvers, others));
	}
}

package daomephsta.unpick.api.classresolvers;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.classresolvers.ChainClassResolver;
import daomephsta.unpick.impl.constantresolvers.BytecodeAnalysisConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.BytecodeAnalysisInheritanceChecker;
import daomephsta.unpick.impl.membercheckers.BytecodeAnalysisMemberChecker;

/**
 * Resolves classes as {@link ClassNode}s, by their internal name.
 * @author Daomephsta
 */
public interface IClassResolver {
	/**
	 * @param internalName the internal name of the class to resolve
	 * @return a {@link ClassNode} for the resolved class, or {@code null} if not found
	 */
	@Nullable
	ClassNode resolveClass(String internalName);

	default IConstantResolver asConstantResolver() {
		return new BytecodeAnalysisConstantResolver(this);
	}

	default IInheritanceChecker asInheritanceChecker() {
		return new BytecodeAnalysisInheritanceChecker(this);
	}

	default IMemberChecker asMemberChecker() {
		return new BytecodeAnalysisMemberChecker(this);
	}

	default IClassResolver chain(IClassResolver... others) {
		return new ChainClassResolver(Utils.prepend(this, others));
	}
}

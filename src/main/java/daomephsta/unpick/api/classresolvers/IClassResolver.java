package daomephsta.unpick.api.classresolvers;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.classresolvers.ChainClassResolver;
import daomephsta.unpick.impl.constantresolvers.BytecodeAnalysisConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.BytecodeAnalysisInheritanceChecker;
import daomephsta.unpick.impl.membercheckers.BytecodeAnalysisMemberChecker;

/**
 * Resolves classes as {@link ClassReader}s, by their internal name.
 * @author Daomephsta
 */
public interface IClassResolver {
	/**
	 * @param internalName the internal name of the class to resolve
	 * @return a {@link ClassReader} for the resolved class, or {@code null} if not found
	 */
	@Nullable
	ClassReader resolveClass(String internalName);

	@Nullable
	default ClassNode resolveClassNode(String internalName, int readerFlags) {
		return null;
	}

	default ClassNode resolveClassNodeCreating(String internalName, int readerFlags) {
		ClassNode classNode = resolveClassNode(internalName, readerFlags);
		if (classNode == null) {
			ClassReader classReader = resolveClass(internalName);
			if (classReader == null) {
				return null;
			}
			classNode = new ClassNode();
			classReader.accept(classNode, readerFlags);
		}
		return classNode;
	}

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

package daomephsta.unpick.api.constantresolvers;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.impl.constantresolvers.BytecodeAnalysisConstantResolver;

/**
 * API methods for creating instances of predefined implementations of {@link IConstantResolver}.
 * @author Daomephsta
 */
public class ConstantResolvers {
	/**
	 * @param classResolver a class resolver for resolving the constants' owner classes.
	 * @return a constant resolver that resolves constants by inspecting the bytecode of their
	 * owning classes.
	 */
	public static IConstantResolver bytecodeAnalysis(IClassResolver classResolver) {
		return new BytecodeAnalysisConstantResolver(classResolver);
	}
}

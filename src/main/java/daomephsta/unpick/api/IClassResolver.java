package daomephsta.unpick.api;

import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.api.inheritancecheckers.IInheritanceChecker;
import daomephsta.unpick.impl.constantresolvers.BytecodeAnalysisConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.BytecodeAnalysisInheritanceChecker;
import org.objectweb.asm.ClassReader;

/**
 * Resolves classes as {@link ClassReader}s, by their internal name
 * @author Daomephsta
 */
public interface IClassResolver
{
	/**
	 * @param internalName the internal name of the class to resolve
	 * @return a {@link ClassReader} for the resolved class
	 * @throws ClassResolutionException if construction of the ClassReader throws an IOException
	 * or no class can be found with the specified internal name.
	 */
	ClassReader resolveClass(String internalName) throws ClassResolutionException;

	default IConstantResolver asConstantResolver()
	{
		return new BytecodeAnalysisConstantResolver(this);
	}

	default IInheritanceChecker asInheritanceChecker()
	{
		return new BytecodeAnalysisInheritanceChecker(this);
	}
	
	public static class ClassResolutionException extends RuntimeException
	{
		private static final long serialVersionUID = 4617765695823272821L;

		public ClassResolutionException(String message, Throwable cause)
		{
			super(message, cause);
		}

		public ClassResolutionException(String message)
		{
			super(message);
		}

		public ClassResolutionException(Throwable cause)
		{
			super(cause);
		}
	}
}

package daomephsta.unpick.api;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * Resolves classes as {@link ClassReader}s, by their internal name
 * @author Daomephsta
 */
public interface IClassResolver
{
	/**
	 * @param binaryName the binary name of the class to resolve
	 * @return a {@link ClassReader} for the resolved class
	 * @throws ClassResolutionException if construction of the ClassReader throws an IOException
	 * or no class can be found with the specified binary name.
	 */
	public ClassReader resolveClassReader(String binaryName) throws ClassResolutionException;
	
	/**
	 * @param binaryName the binary name of the class to resolve
	 * @return a {@link ClassNode} for the resolved class. If {@code a.equals(b)} then 
	 * it must be true that<br>{@code resolveClassNode(a) == resolveClassNode(b)}.
	 * @throws ClassResolutionException if construction of the ClassReader throws an IOException
	 * or no class can be found with the specified binary name.
	 */
	public ClassNode resolveClassNode(String binaryName) throws ClassResolutionException;
	
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

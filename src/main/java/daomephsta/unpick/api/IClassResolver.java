package daomephsta.unpick.api;

import org.objectweb.asm.ClassReader;

/**
 * Resolves classes as {@link ClassReader}s, by their internal name.
 * @author Daomephsta
 */
public interface IClassResolver {
	/**
	 * @param internalName the internal name of the class to resolve
	 * @return a {@link ClassReader} for the resolved class
	 * @throws ClassResolutionException if construction of the ClassReader throws an IOException
	 * or no class can be found with the specified internal name.
	 */
	ClassReader resolveClass(String internalName) throws ClassResolutionException;

	class ClassResolutionException extends RuntimeException {
		private static final long serialVersionUID = 4617765695823272821L;

		public ClassResolutionException(String message, Throwable cause) {
			super(message, cause);
		}

		public ClassResolutionException(String message) {
			super(message);
		}

		public ClassResolutionException(Throwable cause) {
			super(cause);
		}
	}
}

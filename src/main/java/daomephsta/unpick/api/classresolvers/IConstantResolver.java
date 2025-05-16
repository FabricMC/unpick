package daomephsta.unpick.api.classresolvers;

import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.constantresolvers.ChainConstantResolver;

/**
 * Defines a method of resolving constants by their owning class and name.
 * @author Daomephsta
 */
public interface IConstantResolver {
	/**
	 * Immutable data object that holds information about a resolved constant.
	 *
	 * <p>If {@link #type} is {@link Class}, then {@link #value} will be an instance of {@link Type}. Otherwise, the
	 * type of {@code value} will match {@code type}. {@code value} may be {@code null} if {@code type} is not a
	 * primitive type.
	 *
	 * @author Daomephsta
	 */
	record ResolvedConstant(Type type, @Nullable Object value, boolean isStatic) {
	}

	/**
	 * Resolves the type and value of a constant
	 * from its owning class and name.
	 * @param owner the internal name of the class that owns the constant.
	 * @param name the name of the constant.
	 * @return the type and value of the constant as an instance of {@link ResolvedConstant}.
	 */
	@Nullable
	ResolvedConstant resolveConstant(String owner, String name);

	/**
	 * Retrieves all constants declared within the specified class.
	 *
	 * @param owner the internal name of the class whose constants are to be retrieved
	 * @return a map where the keys are the names of the constants and the values are their corresponding
	 *         {@link ResolvedConstant} instances, or {@code null} if the class cannot be resolved
	 */
	@Nullable
	Map<String, ResolvedConstant> getAllConstantsInClass(String owner);

	default IConstantResolver chain(IConstantResolver... others) {
		return new ChainConstantResolver(Utils.prepend(this, others));
	}
}

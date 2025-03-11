package daomephsta.unpick.api.classresolvers;

import java.util.AbstractMap;

import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.constantresolvers.ChainConstantResolver;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

/**
 * Defines a method of resolving constants by their owning class and name.
 * @author Daomephsta
 */
public interface IConstantResolver
{
	/**
	 * Immutable data object that holds information about a resolved constant.
	 * @author Daomephsta
	 */
	public static class ResolvedConstant extends AbstractMap.SimpleImmutableEntry<Type, Object>
	{
		public ResolvedConstant(Type type, Object value)
		{
			super(type, value);
		}

		public Type getType()
		{
			return super.getKey();
		}

		@Override
		public Object getValue()
		{
			return super.getValue();
		}

		@Override
		public String toString()
		{
			return String.format("ResolvedConstant [Type Descriptor: %s, Value: %s]", getType(), getValue());
		}
	}

	/**
	 * Resolves the type and value of a constant 
	 * from its owning class and name.
	 * @param owner the internal name of the class that owns the constant.
	 * @param name the name of the constant.
	 * @return the type and value of the constant as an instance of {@link ResolvedConstant}.
	 */
	@Nullable
	public ResolvedConstant resolveConstant(String owner, String name);

	default IConstantResolver chain(IConstantResolver... others)
	{
		return new ChainConstantResolver(Utils.prepend(this, others));
	}
}

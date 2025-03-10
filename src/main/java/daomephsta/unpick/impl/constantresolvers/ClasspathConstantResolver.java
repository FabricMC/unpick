package daomephsta.unpick.impl.constantresolvers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import daomephsta.unpick.impl.classresolvers.ClasspathClassResolver;

import org.objectweb.asm.Type;

/**
 * Resolves constants by looking for them on the classpath.
 * @author Daomephsta
 */
public class ClasspathConstantResolver extends BytecodeAnalysisConstantResolver
{
	public ClasspathConstantResolver()
	{
		super(new ClasspathClassResolver());
	}

	@Override
	public ResolvedConstant resolveConstant(String owner, String name)
	{
		ResolvedConstant resolvedConstant = super.resolveConstant(owner, name);
		if (resolvedConstant != null)
		{
			return resolvedConstant;
		}

		// Fallback: use reflection (but this means we are unable to tell whether a field is constant, and doesn't
		// work for instance fields)
		Class<?> clazz;
		try
		{
			clazz = Class.forName(owner.replace('/', '.'), false, ClasspathConstantResolver.class.getClassLoader());
		}
		catch (ClassNotFoundException e)
		{
			return null;
		}

		Field field;
		try
		{
			field = clazz.getDeclaredField(name);
		}
		catch (NoSuchFieldException e)
		{
			return null;
		}
		final int staticFinal = Modifier.STATIC | Modifier.FINAL;
		if ((field.getModifiers() & staticFinal) != staticFinal)
		{
			return null;
		}

		Type type = Type.getType(field.getType());
		if (!VALID_CONSTANT_TYPES.contains(type))
		{
			return null;
		}

		field.setAccessible(true);
		Object value;
		try
		{
			value = field.get(null);
		}
		catch (IllegalAccessException e)
		{
			return null;
		}

		if (value instanceof Class)
		{
			value = Type.getType((Class<?>) value);
		}

		return new ResolvedConstant(type, value);
	}
}
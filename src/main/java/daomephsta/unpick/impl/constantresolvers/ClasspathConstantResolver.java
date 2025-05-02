package daomephsta.unpick.impl.constantresolvers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import daomephsta.unpick.impl.classresolvers.ClasspathClassResolver;

/**
 * Resolves constants by looking for them on the classpath.
 * @author Daomephsta
 */
public class ClasspathConstantResolver extends BytecodeAnalysisConstantResolver {
	@Nullable
	private final ClassLoader classLoader;

	public ClasspathConstantResolver(@Nullable ClassLoader classLoader) {
		super(new ClasspathClassResolver(classLoader));
		this.classLoader = classLoader;
	}

	@Override
	public ResolvedConstant resolveConstant(String owner, String name) {
		ResolvedConstant resolvedConstant = super.resolveConstant(owner, name);
		if (resolvedConstant != null) {
			return resolvedConstant;
		}

		// Fallback: use reflection (but this means we are unable to tell whether a field is constant, and doesn't
		// work for instance fields)
		Class<?> clazz;
		try {
			clazz = Class.forName(owner.replace('/', '.'), false, classLoader);
		} catch (ClassNotFoundException e) {
			return null;
		}

		Field field;
		try {
			field = clazz.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			return null;
		}

		return resolvedConstantFromField(field);
	}

	@Override
	public Map<String, ResolvedConstant> getAllConstantsInClass(String owner) {
		Map<String, ResolvedConstant> resolvedConstants = super.getAllConstantsInClass(owner);
		if (resolvedConstants != null) {
			return resolvedConstants;
		}

		// Fallback: use reflection (but this means we are unable to tell whether a field is a constant, and doesn't
		// work for instance fields)
		Class<?> clazz;
		try {
			clazz = Class.forName(owner.replace('/', '.'), false, classLoader);
		} catch (ClassNotFoundException e) {
			return null;
		}

		Map<String, ResolvedConstant> constants = new HashMap<>();
		for (Field field : clazz.getDeclaredFields()) {
			ResolvedConstant resolvedConstant = resolvedConstantFromField(field);
			if (resolvedConstant != null) {
				constants.put(field.getName(), resolvedConstant);
			}
		}

		return constants;
	}

	@Nullable
	private static ResolvedConstant resolvedConstantFromField(Field field) {
		final int staticFinal = Modifier.STATIC | Modifier.FINAL;
		if ((field.getModifiers() & staticFinal) != staticFinal) {
			return null;
		}

		Type type = Type.getType(field.getType());
		if (!VALID_CONSTANT_TYPES.contains(type)) {
			return null;
		}

		field.setAccessible(true);
		Object value;
		try {
			value = field.get(null);
		} catch (IllegalAccessException e) {
			return null;
		}

		if (value instanceof Class) {
			value = Type.getType((Class<?>) value);
		}

		return new ResolvedConstant(type, value, true);
	}
}

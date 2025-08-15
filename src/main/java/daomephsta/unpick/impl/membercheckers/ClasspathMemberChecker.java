package daomephsta.unpick.impl.membercheckers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import daomephsta.unpick.api.classresolvers.IMemberChecker;

public class ClasspathMemberChecker implements IMemberChecker {
	@Nullable
	private final ClassLoader classLoader;

	public ClasspathMemberChecker(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	@Nullable
	public List<MemberInfo> getFields(String className) {
		Class<?> clazz = findClass(className);
		if (clazz == null) {
			return null;
		}

		return Arrays.stream(clazz.getDeclaredFields()).map(ClasspathMemberChecker::fieldToMemberInfo).toList();
	}

	@Override
	@Nullable
	public List<MemberInfo> getMethods(String className) {
		Class<?> clazz = findClass(className);
		if (clazz == null) {
			return null;
		}

		return Stream.concat(
				Arrays.stream(clazz.getDeclaredConstructors()).map(ClasspathMemberChecker::constructorToMemberInfo),
				Arrays.stream(clazz.getDeclaredMethods()).map(ClasspathMemberChecker::methodToMemberInfo)
		).toList();
	}

	@Override
	@Nullable
	public MemberInfo getField(String className, String fieldName, String fieldDesc) {
		Class<?> clazz = findClass(className);
		if (clazz == null) {
			return null;
		}

		try {
			Field field = clazz.getDeclaredField(fieldName);
			if (!Type.getDescriptor(field.getType()).equals(fieldDesc)) {
				return null;
			}

			return fieldToMemberInfo(field);
		} catch (NoSuchFieldException e) {
			return null;
		}
	}

	@Nullable
	private Class<?> findClass(String name) {
		try {
			return Class.forName(name.replace('/', '.'), false, classLoader);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private static MemberInfo fieldToMemberInfo(Field field) {
		return new MemberInfo(
				field.getModifiers(),
				field.getName(),
				Type.getDescriptor(field.getType())
		);
	}

	private static MemberInfo methodToMemberInfo(Method method) {
		return new MemberInfo(
				method.getModifiers(),
				method.getName(),
				Type.getMethodDescriptor(method)
		);
	}

	private static MemberInfo constructorToMemberInfo(Constructor<?> constructor) {
		return new MemberInfo(
				constructor.getModifiers(),
				"<init>",
				Type.getConstructorDescriptor(constructor)
		);
	}
}

package daomephsta.unpick.impl.membercheckers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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

	@Override
	@Nullable
	public ParameterInfo getParameter(String className, String methodName, String methodDesc, int parameterIndex) {
		Class<?> clazz = findClass(className);
		if (clazz == null) {
			return null;
		}

		Executable method;
		if ("<init>".equals(methodName)) {
			method = Arrays.stream(clazz.getDeclaredConstructors())
					.filter(ctor -> Type.getConstructorDescriptor(ctor).equals(methodDesc))
					.findAny()
					.orElse(null);
		} else {
			method = Arrays.stream(clazz.getDeclaredMethods())
					.filter(md -> md.getName().equals(methodName) && Type.getMethodDescriptor(md).equals(methodDesc))
					.findAny()
					.orElse(null);
		}

		if (method == null) {
			return null;
		}

		Parameter[] parameters = method.getParameters();
		if (parameterIndex >= parameters.length) {
			return null;
		}

		return parameterToParameterInfo(parameters[parameterIndex]);
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
		return MemberInfo.create(
				field.getModifiers(),
				field.getName(),
				Type.getDescriptor(field.getType())
		).withAnnotations(convertAnnotations(field.getAnnotations()));
	}

	private static MemberInfo methodToMemberInfo(Method method) {
		return MemberInfo.create(
				method.getModifiers(),
				method.getName(),
				Type.getMethodDescriptor(method)
		).withAnnotations(convertAnnotations(method.getAnnotations()));
	}

	private static MemberInfo constructorToMemberInfo(Constructor<?> constructor) {
		return MemberInfo.create(
				constructor.getModifiers(),
				"<init>",
				Type.getConstructorDescriptor(constructor)
		).withAnnotations(convertAnnotations(constructor.getAnnotations()));
	}

	private static ParameterInfo parameterToParameterInfo(Parameter parameter) {
		return ParameterInfo.create(parameter.getModifiers())
				.withAnnotations(convertAnnotations(parameter.getAnnotations()));
	}

	private static List<String> convertAnnotations(Annotation[] annotations) {
		return Arrays.stream(annotations).map(ann -> ann.annotationType().getName().replace('.', '/')).toList();
	}
}

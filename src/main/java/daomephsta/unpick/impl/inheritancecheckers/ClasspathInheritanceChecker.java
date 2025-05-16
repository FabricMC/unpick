package daomephsta.unpick.impl.inheritancecheckers;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.api.classresolvers.IInheritanceChecker;

public class ClasspathInheritanceChecker implements IInheritanceChecker {
	@Nullable
	private final ClassLoader classLoader;

	public ClasspathInheritanceChecker(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	@Nullable
	public ClassInfo getClassInfo(String className) {
		Class<?> clazz;
		try {
			clazz = Class.forName(className.replace('/', '.'), false, classLoader);
		} catch (ClassNotFoundException e) {
			return null;
		}

		Class<?> superclass = clazz.isInterface() ? Object.class : clazz.getSuperclass();
		String superclassName = superclass == null ? null : superclass.getName().replace('.', '/');

		Class<?>[] interfaces = clazz.getInterfaces();
		String[] interfaceNames = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			interfaceNames[i] = interfaces[i].getName().replace('.', '/');
		}

		return new ClassInfo(superclassName, interfaceNames, clazz.isInterface());
	}
}

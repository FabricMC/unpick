package daomephsta.unpick.impl.inheritancecheckers;

import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import org.jetbrains.annotations.Nullable;

public class ClasspathInheritanceChecker implements IInheritanceChecker
{
	@Override
	@Nullable
	public ClassInfo getClassInfo(String className)
	{
		Class<?> clazz;
		try
		{
			clazz = Class.forName(className.replace('/', '.'), false, ClasspathInheritanceChecker.class.getClassLoader());
		}
		catch (ClassNotFoundException e)
		{
			return null;
		}

		Class<?> superclass = clazz.isInterface() ? Object.class : clazz.getSuperclass();
		String superclassName = superclass == null ? null : superclass.getName().replace('.', '/');

		Class<?>[] interfaces = clazz.getInterfaces();
		String[] interfaceNames = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++)
		{
			interfaceNames[i] = interfaces[i].getName().replace('.', '/');
		}

		return new ClassInfo(superclassName, interfaceNames, clazz.isInterface());
	}
}

package daomephsta.unpick.api.classresolvers;

import daomephsta.unpick.impl.classresolvers.ClasspathClassResolver;
import daomephsta.unpick.impl.classresolvers.JarClassResolver;
import daomephsta.unpick.impl.classresolvers.PathClassResolver;

import java.nio.file.Path;
import java.util.zip.ZipFile;

public final class ClassResolvers
{
	private ClassResolvers()
	{
	}

	public static IClassResolver classpath()
	{
		return new ClasspathClassResolver();
	}

	public static IClassResolver fromPath(Path root)
	{
		return new PathClassResolver(root);
	}

	public static IClassResolver jar(ZipFile jarFile)
	{
		return new JarClassResolver(jarFile);
	}
}

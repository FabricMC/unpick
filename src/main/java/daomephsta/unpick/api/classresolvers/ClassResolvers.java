package daomephsta.unpick.api.classresolvers;

import java.nio.file.Path;
import java.util.zip.ZipFile;

import daomephsta.unpick.impl.classresolvers.ClasspathClassResolver;
import daomephsta.unpick.impl.classresolvers.JarClassResolver;
import daomephsta.unpick.impl.classresolvers.PathClassResolver;

public final class ClassResolvers {
	private ClassResolvers() {
	}

	public static IClassResolver classpath() {
		return classpath(Thread.currentThread().getContextClassLoader());
	}

	public static IClassResolver classpath(ClassLoader classLoader) {
		return new ClasspathClassResolver(classLoader);
	}

	public static IClassResolver fromDirectory(Path root) {
		return new PathClassResolver(root);
	}

	public static IClassResolver jar(ZipFile jarFile) {
		return new JarClassResolver(jarFile);
	}
}

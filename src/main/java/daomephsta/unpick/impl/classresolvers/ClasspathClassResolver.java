package daomephsta.unpick.impl.classresolvers;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.impl.constantresolvers.ClasspathConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.ClasspathInheritanceChecker;

public class ClasspathClassResolver implements IClassResolver {
	@Nullable
	private final ClassLoader classLoader;

	public ClasspathClassResolver(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	@Nullable
	public ClassReader resolveClass(String internalName) {
		String resourceName = internalName + ".class";
		try (InputStream is = classLoader == null ? ClassLoader.getSystemResourceAsStream(resourceName) : classLoader.getResourceAsStream(resourceName)) {
			if (is == null) {
				return null;
			}
			return new ClassReader(is);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public IConstantResolver asConstantResolver() {
		return new ClasspathConstantResolver(classLoader);
	}

	@Override
	public IInheritanceChecker asInheritanceChecker() {
		return new ClasspathInheritanceChecker(classLoader);
	}
}

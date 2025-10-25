package daomephsta.unpick.impl.classresolvers;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.api.classresolvers.IMemberChecker;
import daomephsta.unpick.impl.constantresolvers.ClasspathConstantResolver;
import daomephsta.unpick.impl.inheritancecheckers.ClasspathInheritanceChecker;
import daomephsta.unpick.impl.membercheckers.ClasspathMemberChecker;

public class ClasspathClassResolver implements IClassResolver {
	@Nullable
	private final ClassLoader classLoader;

	public ClasspathClassResolver(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	@Nullable
	public ClassNode resolveClass(String internalName) {
		String resourceName = internalName + ".class";
		try (InputStream is = classLoader == null ? ClassLoader.getSystemResourceAsStream(resourceName) : classLoader.getResourceAsStream(resourceName)) {
			if (is == null) {
				return null;
			}
			ClassReader classReader = new ClassReader(is);
			ClassNode classNode = new ClassNode();
			classReader.accept(classNode, ClassReader.SKIP_DEBUG);
			return classNode;
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

	@Override
	public IMemberChecker asMemberChecker() {
		return new ClasspathMemberChecker(classLoader);
	}
}

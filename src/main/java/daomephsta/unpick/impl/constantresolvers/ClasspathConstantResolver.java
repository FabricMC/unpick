package daomephsta.unpick.impl.constantresolvers;

import java.io.IOException;

import org.objectweb.asm.ClassReader;

import daomephsta.unpick.api.IClassResolver;

/**
 * Resolves constants by looking for them on the classpath.
 * @author Daomephsta
 */
public class ClasspathConstantResolver extends BytecodeAnalysisConstantResolver {
	public ClasspathConstantResolver() {
		super(new IClassResolver() {
			@Override
			public ClassReader resolveClass(String internalName) throws ClassResolutionException {
				try {
					return new ClassReader(internalName);
				} catch (IOException e) {
					throw new ClassResolutionException(e);
				}
			}
		});
	}
}

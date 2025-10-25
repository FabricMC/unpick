package daomephsta.unpick.impl.classresolvers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;

public class PathClassResolver implements IClassResolver {
	private final Path root;

	public PathClassResolver(Path root) {
		this.root = root;
	}

	@Override
	@Nullable
	public ClassNode resolveClass(String internalName) {
		try (InputStream is = Files.newInputStream(root.resolve(internalName + ".class"))) {
			ClassReader classReader = new ClassReader(is);
			ClassNode classNode = new ClassNode();
			classReader.accept(classNode, ClassReader.SKIP_DEBUG);
			return classNode;
		} catch (IOException e) {
			return null;
		}
	}
}

package daomephsta.unpick.impl.classresolvers;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;

public class JarClassResolver implements IClassResolver {
	private final ZipFile zipFile;

	public JarClassResolver(ZipFile zipFile) {
		this.zipFile = zipFile;
	}

	@Override
	@Nullable
	public ClassNode resolveClass(String internalName) {
		ZipEntry entry = zipFile.getEntry(internalName + ".class");
		if (entry == null) {
			return null;
		}

		try (InputStream is = zipFile.getInputStream(entry)) {
			ClassReader classReader = new ClassReader(is);
			ClassNode classNode = new ClassNode();
			classReader.accept(classNode, ClassReader.SKIP_DEBUG);
			return classNode;
		} catch (IOException e) {
			return null;
		}
	}
}

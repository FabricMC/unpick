package daomephsta.unpick.impl.classresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;

import org.jetbrains.annotations.Nullable;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathClassResolver implements IClassResolver
{
	private final Path root;

	public PathClassResolver(Path root)
	{
		this.root = root;
	}

	@Override
	@Nullable
	public ClassReader resolveClass(String internalName)
	{
		try (InputStream is = Files.newInputStream(root.resolve(internalName + ".class")))
		{
			return new ClassReader(is);
		}
		catch (IOException e)
		{
			return null;
		}
	}
}

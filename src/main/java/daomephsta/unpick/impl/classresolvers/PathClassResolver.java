package daomephsta.unpick.impl.classresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
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
	public ClassReader resolveClass(String internalName) throws ClassResolutionException
	{
		try (InputStream is = Files.newInputStream(root.resolve(internalName + ".class")))
		{
			return new ClassReader(is);
		}
		catch (IOException e)
		{
			throw new ClassResolutionException(e);
		}
	}
}

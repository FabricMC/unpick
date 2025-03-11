package daomephsta.unpick.impl.classresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarClassResolver implements IClassResolver
{
	private final ZipFile zipFile;

	public JarClassResolver(ZipFile zipFile)
	{
		this.zipFile = zipFile;
	}

	@Override
	public ClassReader resolveClass(String internalName) throws ClassResolutionException
	{
		ZipEntry entry = zipFile.getEntry(internalName + ".class");
		if (entry == null)
		{
			throw new ClassResolutionException(internalName);
		}

		try (InputStream is = zipFile.getInputStream(entry))
		{
			return new ClassReader(is);
		}
		catch (IOException e)
		{
			throw new ClassResolutionException(e);
		}
	}
}

package daomephsta.unpick.impl.classresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;

import org.jetbrains.annotations.Nullable;

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
	@Nullable
	public ClassReader resolveClass(String internalName)
	{
		ZipEntry entry = zipFile.getEntry(internalName + ".class");
		if (entry == null)
		{
			return null;
		}

		try (InputStream is = zipFile.getInputStream(entry))
		{
			return new ClassReader(is);
		}
		catch (IOException e)
		{
			return null;
		}
	}
}

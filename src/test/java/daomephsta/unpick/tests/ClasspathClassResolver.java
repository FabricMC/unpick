package daomephsta.unpick.tests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.api.IClassResolver;

class ClasspathClassResolver implements IClassResolver
{
	private final Map<String, ClassNode> cache = new HashMap<>();

	@Override
	public ClassReader resolveClassReader(String internalName) throws ClassResolutionException
	{
		try 
		{
			return new ClassReader(internalName);
		} 
		catch (IOException e) 
		{
			throw new IClassResolver.ClassResolutionException(e);
		}
	}

	@Override
	public ClassNode resolveClassNode(String internalName) throws ClassResolutionException
	{
		return cache.computeIfAbsent(internalName, name -> 
		{
			ClassNode node = new ClassNode();
			resolveClassReader(name).accept(node, ClassReader.SKIP_DEBUG);
			return node;
		});
	}
}
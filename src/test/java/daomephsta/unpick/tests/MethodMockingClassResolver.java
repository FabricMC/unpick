package daomephsta.unpick.tests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.tests.lib.MethodMocker;
import daomephsta.unpick.tests.lib.MethodMocker.MockMethod;

class MockClassResolver implements IClassResolver
{
	private final Map<String, ClassNode> cache = new HashMap<>();

	public MethodNode mock(MockMethod mock)
	{
		if (cache.putIfAbsent(MethodMocker.CLASS_NAME, mock.getMockClass()) != null)
			throw new IllegalStateException("Mock class in use");
		return mock.getMockMethod();
	}
	
	@Override
	public ClassReader resolveClassReader(String className) throws ClassResolutionException
	{
		try 
		{
			return new ClassReader(className);
		} 
		catch (IOException e) 
		{
			throw new IClassResolver.ClassResolutionException(e);
		}
	}

	@Override
	public ClassNode resolveClassNode(String className) throws ClassResolutionException
	{
		return cache.computeIfAbsent(className, name -> 
		{
			ClassNode node = new ClassNode();
			resolveClassReader(name).accept(node, ClassReader.SKIP_DEBUG);
			return node;
		});
	}
}
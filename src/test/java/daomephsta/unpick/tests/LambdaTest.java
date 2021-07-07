package daomephsta.unpick.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.impl.constantresolvers.ClasspathConstantResolver;
import daomephsta.unpick.tests.lib.ASMAssertions;
import daomephsta.unpick.tests.lib.MockConstantMapper;

public class LambdaTest
{
	private static final IClassResolver CLASS_RESOLVER = internalName -> {
		try {
			return new ClassReader(internalName);
		} catch (IOException e) {
			throw new IClassResolver.ClassResolutionException(e);
		}
	};
	private static final int MINUS_1 = -1, 
							 ARBITRARY = 257;

	private static Stream<Arguments> internalLambdaConstantReturn()
	{
		return Stream.of(
			arguments(MINUS_1, "MINUS_1"),
			arguments(ARBITRARY, "ARBITRARY")
		);
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource
	public void internalLambdaConstantReturn(int value, String constantName) throws IOException 
	{
		IConstantMapper mapper = MockConstantMapper.builder(CLASS_RESOLVER)
			.simpleConstantGroup("test")
				.defineAll(this.getClass(), constantName)
				.add()
			.targetMethod(LambdaI2V.class, "getInt", "()I")
				.remapReturn("test")
				.add()
			.build();
		ConstantUninliner uninliner = new ConstantUninliner(mapper, new ClasspathConstantResolver());
		ClassNode testClass = getClassNode(Methods.class);
		MethodNode lambda = findLambda(testClass, "lambdaParent" + constantName);
		ASMAssertions.assertIsLiteral(lambda.instructions.get(0), value);
		uninliner.transform(testClass);
		ASMAssertions.assertReadsField(lambda.instructions.get(0), this.getClass(), constantName, "I");
	}
	
	@Test
	public void externalLambdaConstantReturn() throws IOException 
	{
		IConstantMapper mapper = MockConstantMapper.builder(CLASS_RESOLVER)
			.simpleConstantGroup("test")
				.defineAll(this.getClass(), "ARBITRARY")
				.add()
			.targetMethod(LambdaI2V.class, "getInt", "()I")
				.remapReturn("test")
				.add()
			.build();
		ConstantUninliner uninliner = new ConstantUninliner(mapper, new ClasspathConstantResolver());
		ClassNode lambdaParentClass = getClassNode(Methods.class);
		MethodNode lambda = findLambda(lambdaParentClass, "lambdaParentExternal");
		ASMAssertions.assertIsLiteral(lambda.instructions.get(0), ARBITRARY);
		uninliner.transform(lambdaParentClass);
		ASMAssertions.assertReadsField(lambda.instructions.get(0), this.getClass(), "ARBITRARY", "I");
	}

	private static ClassNode getClassNode(Class<?> clazz) throws IOException
	{
		return getClassNode(clazz.getName());
	}

	private static ClassNode getClassNode(String className)
	{
		ClassNode node = new ClassNode();
		CLASS_RESOLVER.resolveClass(className).accept(node, ClassReader.SKIP_DEBUG);
		return node;
	}

	private static MethodNode findLambda(ClassNode lambdaParentClass, String lambdaParentName)
	{
		MethodNode lambdaParent = null;
		for (MethodNode method : lambdaParentClass.methods)
		{
			if (method.name.equals(lambdaParentName))
				lambdaParent = method;
		}
		assertNotNull(lambdaParent, "Lambda parent " + lambdaParentName + " not found");
		
		Handle implementation = null;
		for (AbstractInsnNode insn : lambdaParent.instructions)
		{
			if (insn instanceof InvokeDynamicInsnNode)
			{
				InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) insn;
				implementation = ((Handle)invokeDynamic.bsmArgs[1]);
				break;
			}
		}
		assertNotNull(implementation, "INVOKEDYNAMIC not found in " + lambdaParent.name);
		
		ClassNode lambdaImplClass = getClassNode(implementation.getOwner());
		for (MethodNode method : lambdaImplClass.methods)
		{
			if (method.name.equals(implementation.getName()))
				return method;
		}
		return fail("Lambda " + implementation + " not found");
	}

	@SuppressWarnings("unused")
	private static class Methods
	{ 
		void lambdaParentMINUS_1()
		{
			lambdaConsumer(() -> MINUS_1);
		}
		
		void lambdaParentARBITRARY()
		{
			lambdaConsumer(() -> ARBITRARY);
		}
		
		void lambdaParentExternal()
		{
			lambdaConsumer(ExternalLambda::impl);
		}

		void lambdaConsumer(LambdaI2V getter) {}
	}
	
	private static class ExternalLambda
	{ 
		static int impl()
		{
			return ARBITRARY;
		}
	}

	interface LambdaI2V
	{
		public int getInt();
	}
}

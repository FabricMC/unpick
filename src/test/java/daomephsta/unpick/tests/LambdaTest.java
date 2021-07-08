package daomephsta.unpick.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.impl.constantresolvers.BytecodeAnalysisConstantResolver;
import daomephsta.unpick.tests.lib.ASMAssertions;
import daomephsta.unpick.tests.lib.MockConstantMapper;

public class LambdaTest
{
	private static final int MINUS_1 = -1, 
							 ARBITRARY = 257;

	private static Stream<Arguments> lambdaConstantReturn()
	{
		return Stream.of(
			arguments("lambdaParentMINUS_1", MINUS_1, "MINUS_1"),
			arguments("lambdaParentARBITRARY", ARBITRARY, "ARBITRARY"),
			arguments("staticMethodRefParent", ARBITRARY, "ARBITRARY"),
			arguments("boundInstanceMethodRefParent", ARBITRARY, "ARBITRARY"),
			arguments("passedInstanceMethodRefParent", ARBITRARY, "ARBITRARY")
		);
	}
	
	@ParameterizedTest(name = "{0}: {1} -> {2}")
	@MethodSource
	public void lambdaConstantReturn(String lambdaParentName, int constant, String constantName) throws IOException 
	{
		IClassResolver classResolver = new MockClassResolver();
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
			.simpleConstantGroup("test")
				.defineAll(this.getClass(), constantName)
				.add()
			.targetMethod(LambdaI.class, "getInt", "()I")
				.remapReturn("test")
				.add()
			.targetMethod(LambdaT2I.class, "getInt", "(Ljava/lang/Object;)I")
				.remapReturn("test")
				.add()
			.build();
		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		ClassNode lambdaParentClass = classResolver.resolveClassNode(Methods.class.getName());
		MethodNode lambda = findLambda(classResolver, lambdaParentClass, lambdaParentName);
		ASMAssertions.assertIsLiteral(lambda.instructions.get(0), constant);
		uninliner.transform(Methods.class.getName());
		ASMAssertions.assertReadsField(lambda.instructions.get(0), this.getClass(), constantName, "I");
	}

	// Finds the test lambda contained by the given parent method
	private static MethodNode findLambda(IClassResolver classResolver, ClassNode lambdaParentClass, String lambdaParentName)
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
		
		ClassNode lambdaImplClass = classResolver.resolveClassNode(implementation.getOwner());
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
		
		void staticMethodRefParent()
		{
			lambdaConsumer(ExternalMethodReferences::staticRef);
		}
		
		void boundInstanceMethodRefParent()
		{
			ExternalMethodReferences methodRefs = new ExternalMethodReferences();
			lambdaConsumer(methodRefs::instanceRef);
		}
		
		void passedInstanceMethodRefParent()
		{
			lambdaConsumer(ExternalMethodReferences::instanceRef, new ExternalMethodReferences());
		}

		void lambdaConsumer(LambdaI lambda) {}
		
		<T> void lambdaConsumer(LambdaT2I<T> lambda, T instance) {}
	}
	
	private static class ExternalMethodReferences
	{ 
		static int staticRef()
		{
			return ARBITRARY;
		} 
		
		int instanceRef()
		{
			return ARBITRARY;
		}
	}

	interface LambdaI
	{
		public int getInt();
	}
	
	interface LambdaT2I<T>
	{
		public int getInt(T t);
	}
}

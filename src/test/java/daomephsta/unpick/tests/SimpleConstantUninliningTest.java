package daomephsta.unpick.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Opcodes.RETURN;

import java.io.IOException;
import java.util.stream.Stream;

import daomephsta.unpick.api.IClassResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.MethodNode;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.impl.AbstractInsnNodes;
import daomephsta.unpick.impl.LiteralType;
import daomephsta.unpick.impl.constantresolvers.ClasspathConstantResolver;
import daomephsta.unpick.tests.lib.*;
import daomephsta.unpick.tests.lib.MethodMocker.MockMethod;

public class SimpleConstantUninliningTest
{
	@SuppressWarnings("unused")
	private static class Methods
	{
		private static void intConsumer(int test) {}

		private static void longConsumer(long test) {}

		private static void floatConsumer(float test) {}

		private static void doubleConsumer(double test) {}

		private static void stringConsumer(String test) {}
	}
	
	@SuppressWarnings("unused")
	private static class Constants
	{
		public static final int INT_CONST_M1 = -1,
								INT_CONST_0 = 0,
								INT_CONST_1 = 1,
								INT_CONST_2 = 2,
								INT_CONST_3 = 3,
								INT_CONST_4 = 4,
								INT_CONST_5 = 5,
								INT_CONST = 257;

		public static final long LONG_CONST_0 = 0,
								 LONG_CONST_1 = 1,
								 LONG_CONST = 1234567890;
		
		public static final float FLOAT_CONST_0 = 0F,
								  FLOAT_CONST_1 = 1F,
								  FLOAT_CONST_2 = 2F,
								  FLOAT_CONST = 5.3F;
		
		public static final double DOUBLE_CONST_0 = 0D,
								   DOUBLE_CONST_1 = 1D,
								   DOUBLE_CONST = 5.3D;
		
		public static final String STRING_CONST_FOO = "foo",
						   		   STRING_CONST_BAR = "bar";

		public static final IClassResolver CLASS_RESOLVER = internalName -> {
			try {
				return new ClassReader(internalName);
			} catch (IOException e) {
				throw new IClassResolver.ClassResolutionException(e);
			}
		};
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("knownIntsProvider")
	public void testKnownIntConstantsParameter(Integer constant, String constantName)
	{	
		testKnownConstantParameter(constant, constantName, "intConsumer", "(I)V");
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("knownIntsProvider")
	public void testKnownIntConstantsReturn(Integer constant, String constantName)
	{	
		testKnownConstantReturn(constant, constantName);
	}
	
	private static Stream<Arguments> knownIntsProvider()
	{
		return Stream.of
		(
			Arguments.of(Constants.INT_CONST_M1, "INT_CONST_M1"),
			Arguments.of(Constants.INT_CONST_0, "INT_CONST_0"),
			Arguments.of(Constants.INT_CONST_1, "INT_CONST_1"),
			Arguments.of(Constants.INT_CONST_2, "INT_CONST_2"),
			Arguments.of(Constants.INT_CONST_3, "INT_CONST_3"),
			Arguments.of(Constants.INT_CONST_4, "INT_CONST_4"),
			Arguments.of(Constants.INT_CONST_5, "INT_CONST_5")
		);
	}
	
	@ParameterizedTest(name = "{0}")
	@ValueSource(ints = {8, 13, 42, -1, -7, -23})
	public void testUnknownIntConstantsParameter(Integer constant)
	{
		testUnknownConstantParameter(constant, "intConsumer", "(I)V");
	}
	
	@ParameterizedTest(name = "{0}")
	@ValueSource(ints = {8, 13, 42, -1, -7, -23})
	public void testUnknownIntConstantsReturn(Integer constant)
	{
		testUnknownConstantReturn(constant);
	}
	
	@ParameterizedTest(name = "{0}L -> {1}")
	@MethodSource("knownLongsProvider")
	public void testKnownLongConstantsParameter(Long constant, String constantName)
	{
		testKnownConstantParameter(constant, constantName, "longConsumer", "(J)V");
	}
	
	@ParameterizedTest(name = "{0}L -> {1}")
	@MethodSource("knownLongsProvider")
	public void testKnownLongConstantsReturn(Long constant, String constantName)
	{
		testKnownConstantReturn(constant, constantName);
	}
	
	private static Stream<Arguments> knownLongsProvider()
	{
		return Stream.of
		(
			Arguments.of(Constants.LONG_CONST_0, "LONG_CONST_0"), 
			Arguments.of(Constants.LONG_CONST_1, "LONG_CONST_1"),
			Arguments.of(Constants.LONG_CONST, "LONG_CONST")
		);
	}
	
	@ParameterizedTest(name = "{0}L")
	@ValueSource(longs = {8L, 13L, 42L, -1L, -7L, -23L})
	public void testUnknownLongConstantsParameter(Long constant)
	{	
		testUnknownConstantParameter(constant, "longConsumer", "(J)V");
	}
	
	@ParameterizedTest(name = "{0}L")
	@ValueSource(longs = {8L, 13L, 42L, -1L, -7L, -23L})
	public void testUnknownLongConstantsReturn(Long constant)
	{	
		testUnknownConstantReturn(constant);
	}
	
	@ParameterizedTest(name = "{0}F -> {1}")
	@MethodSource("knownFloatsProvider")
	public void testKnownFloatConstantsParameter(Float constant, String constantName)
	{	
		testKnownConstantParameter(constant, constantName, "floatConsumer", "(F)V");
	}
	
	@ParameterizedTest(name = "{0}F -> {1}")
	@MethodSource("knownFloatsProvider")
	public void testKnownFloatConstantsReturn(Float constant, String constantName)
	{	
		testKnownConstantReturn(constant, constantName);
	}
	
	private static Stream<Arguments> knownFloatsProvider()
	{
		return Stream.of
		(
			Arguments.of(Constants.FLOAT_CONST_0, "FLOAT_CONST_0"), 
			Arguments.of(Constants.FLOAT_CONST_1, "FLOAT_CONST_1"),
			Arguments.of(Constants.FLOAT_CONST_2, "FLOAT_CONST_2"),
			Arguments.of(Constants.FLOAT_CONST, "FLOAT_CONST")
		);
	}
	
	@ParameterizedTest(name = "{0}F")
	@ValueSource(floats = {0.15F, 1.973F, 24.5F, -0.64F, -2.3F, -21.0F})
	public void testUnknownFloatConstantsParameter(Float constant)
	{	
		testUnknownConstantParameter(constant, "floatConsumer", "(F)V");
	}
	
	@ParameterizedTest(name = "{0}F")
	@ValueSource(floats = {0.15F, 1.973F, 24.5F, -0.64F, -2.3F, -21.0F})
	public void testUnknownFloatConstantsReturn(Float constant)
	{	
		testUnknownConstantReturn(constant);
	}
	
	@ParameterizedTest(name = "{0}D -> {1}")
	@MethodSource("knownDoublesProvider")
	public void testKnownDoubleConstantsParameter(Double constant, String constantName)
	{
		testKnownConstantParameter(constant, constantName, "doubleConsumer", "(D)V");
	}
	
	@ParameterizedTest(name = "{0}D -> {1}")
	@MethodSource("knownDoublesProvider")
	public void testKnownDoubleConstantsReturn(Double constant, String constantName)
	{
		testKnownConstantReturn(constant, constantName);
	}
	
	private static Stream<Arguments> knownDoublesProvider()
	{
		return Stream.of
		(
			Arguments.of(Constants.DOUBLE_CONST_0, "DOUBLE_CONST_0"), 
			Arguments.of(Constants.DOUBLE_CONST_1, "DOUBLE_CONST_1"), 
			Arguments.of(Constants.DOUBLE_CONST, "DOUBLE_CONST")
		);
	}
	
	@ParameterizedTest(name = "{0}D")
	@ValueSource(doubles = {0.15D, 1.973D, 24.5D, -0.64D, -2.3D, -21.0D})
	public void testUnknownDoubleConstantsParameter(Double constant)
	{
		testUnknownConstantParameter(constant, "doubleConsumer", "(D)V");
	}
	
	@ParameterizedTest(name = "{0}D")
	@ValueSource(doubles = {0.15D, 1.973D, 24.5D, -0.64D, -2.3D, -21.0D})
	public void testUnknownDoubleConstantsREtrun(Double constant)
	{
		testUnknownConstantReturn(constant);
	}
	
	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@MethodSource("knownStringsProvider")
	public void testKnownStringConstantsParameter(String constant, String constantName)
	{
		testKnownConstantParameter(constant, constantName, "stringConsumer", "(Ljava/lang/String;)V");
	}
	
	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@MethodSource("knownStringsProvider")
	public void testKnownStringConstantsReturn(String constant, String constantName)
	{
		testKnownConstantReturn(constant, constantName);
	}
	
	private static Stream<Arguments> knownStringsProvider()
	{
		return Stream.of
		(
			Arguments.of(Constants.STRING_CONST_FOO, "STRING_CONST_FOO"),
			Arguments.of(Constants.STRING_CONST_BAR, "STRING_CONST_BAR")
		);
	}
	
	@ParameterizedTest(name = "\"{0}\"")
	@ValueSource(strings = {"baz", "QUX", "1_QuZ_3", "PotatoesareGREAT"})
	public void testUnknownStringConstantsParameter(String constant)
	{
		testUnknownConstantParameter(constant, "stringConsumer", "(Ljava/lang/String;)V");
	}
	
	@ParameterizedTest(name = "\"{0}\"")
	@ValueSource(strings = {"baz", "QUX", "1_QuZ_3", "PotatoesareGREAT"})
	public void testUnknownStringConstantsReturn(String constant)
	{
		testUnknownConstantReturn(constant);
	}
	
	@Test
	public void missingField()
	{
		String constantConsumerName = "foo";
		String constantConsumerDescriptor = "(I)V";
		Class<?> fieldClass = Constants.class;
		String fieldName = "DOES_NOT_EXIST";
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER)
				.simpleConstantGroup("test")
					.define(fieldClass, fieldName)
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(mapper, new ClasspathConstantResolver());
		MethodNode mockInvocation = TestUtils.mockInvokeStatic(Methods.class, constantConsumerName, constantConsumerDescriptor, 1)
				.getMockMethod();
		uninliner.transformMethod(MethodMocker.CLASS_NAME, mockInvocation);
	}

	private void testKnownConstantParameter(Object constant, String expectedConstant, String constantConsumerName, String constantConsumerDescriptor)
	{
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER)
				.simpleConstantGroup("test")
					.define(Constants.class, expectedConstant)
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();

		LiteralType literalType = LiteralType.from(constant.getClass());
		ConstantUninliner uninliner = new ConstantUninliner(mapper, new ClasspathConstantResolver());
		MethodNode mockInvocation = TestUtils.mockInvokeStatic(Methods.class, constantConsumerName, constantConsumerDescriptor, 
				constant).getMockMethod();
		int invocationInsnIndex = 1;
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, constant, mockInvocation, 
				invocationInsnIndex);
		uninliner.transformMethod(MethodMocker.CLASS_NAME, mockInvocation);
		ASMAssertions.assertReadsField(mockInvocation.instructions.get(invocationInsnIndex - 1), Constants.class, expectedConstant, 
				literalType.getTypeDescriptor());
	}

	private void testKnownConstantReturn(Object constant, String expectedConstant)
	{
		LiteralType literalType = LiteralType.from(constant.getClass());
		
		MockMethod mock = MethodMocker.mock(literalType.getPrimitiveClass(), mv -> 
		{
			literalType.appendLiteralPushInsn(mv, constant);
			literalType.appendReturnInsn(mv);
		});
		MethodNode mockMethod = mock.getMockMethod();
		
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER)
				.simpleConstantGroup("test")
					.define(Constants.class, expectedConstant)
				.add()
				.targetMethod(mock.getMockClass().name, mockMethod.name, mockMethod.desc)
					.remapReturn("test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(mapper, new ClasspathConstantResolver());

		int returnInsnIndex = 1;
		ASMAssertions.assertIsLiteral(mockMethod.instructions.get(returnInsnIndex - 1), constant);
		ASMAssertions.assertOpcode(mockMethod.instructions.get(returnInsnIndex), literalType.getReturnOpcode());
		uninliner.transformMethod(mock.getMockClass().name, mockMethod);
		ASMAssertions.assertReadsField(mockMethod.instructions.get(returnInsnIndex - 1), Constants.class, expectedConstant, 
				literalType.getTypeDescriptor());
	}
	
	private void testUnknownConstantParameter(Object constant, String constantConsumerName, String constantConsumerDescriptor)
	{
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER)
				.simpleConstantGroup("test")
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(mapper, new ClasspathConstantResolver());
		MethodNode mockInvocation = TestUtils.mockInvokeStatic(Methods.class, constantConsumerName, constantConsumerDescriptor, 
				constant).getMockMethod();
		int invocationInsnIndex = 1;
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, constant, mockInvocation, 
				invocationInsnIndex);
		uninliner.transformMethod(MethodMocker.CLASS_NAME, mockInvocation);
		//Should be unchanged, so this should still pass
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, constant, mockInvocation, 
				invocationInsnIndex);
	}
	
	private void testUnknownConstantReturn(Object constant)
	{
		LiteralType literalType = LiteralType.from(constant.getClass());
		MockMethod mock = MethodMocker.mock(literalType.getPrimitiveClass(), mv -> 
		{
			literalType.appendLiteralPushInsn(mv, constant);
			literalType.appendReturnInsn(mv);
		});
		MethodNode mockMethod = mock.getMockMethod();
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER)
				.simpleConstantGroup("test")
				.add()
				.targetMethod(mock.getMockClass().name, mockMethod.name, mockMethod.desc)
					.remapReturn("test")
				.add()
				.build();
		ConstantUninliner uninliner = new ConstantUninliner(mapper, new ClasspathConstantResolver());

		int returnInsnIndex = 1;
		ASMAssertions.assertIsLiteral(mockMethod.instructions.get(returnInsnIndex - 1), constant);
		ASMAssertions.assertOpcode(mockMethod.instructions.get(returnInsnIndex), literalType.getReturnOpcode());
		uninliner.transformMethod(mock.getMockClass().name, mockMethod);
		//Should be unchanged, so this should still pass
		ASMAssertions.assertIsLiteral(mockMethod.instructions.get(returnInsnIndex - 1), constant);
		ASMAssertions.assertOpcode(mockMethod.instructions.get(returnInsnIndex), literalType.getReturnOpcode());
	}

	private void checkMockInvocationStructure(String constantConsumerName,
			String constantConsumerDescriptor, Object expectedLiteralValue,
			MethodNode mockInvocation, int invocationInsnIndex)
	{
		int expectedInstructionCount = 3;
		assertEquals(expectedInstructionCount, mockInvocation.instructions.size(), 
				String.format("Expected %d instructions, found %d", expectedInstructionCount, mockInvocation.instructions.size()));
		assertEquals(expectedLiteralValue, AbstractInsnNodes.getLiteralValue(mockInvocation.instructions.get(invocationInsnIndex - 1)));
		ASMAssertions.assertInvokesMethod(mockInvocation.instructions.get(invocationInsnIndex), Methods.class, 
				constantConsumerName, constantConsumerDescriptor);
		ASMAssertions.assertOpcode(mockInvocation.instructions.get(invocationInsnIndex + 1), RETURN);
	}
}

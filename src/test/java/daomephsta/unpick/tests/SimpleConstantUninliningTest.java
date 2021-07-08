package daomephsta.unpick.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.impl.LiteralType;
import daomephsta.unpick.impl.constantresolvers.BytecodeAnalysisConstantResolver;
import daomephsta.unpick.tests.lib.ASMAssertions;
import daomephsta.unpick.tests.lib.MethodMocker;
import daomephsta.unpick.tests.lib.MethodMocker.MockMethod;
import daomephsta.unpick.tests.lib.MockConstantMapper;
import daomephsta.unpick.tests.lib.TestUtils;

public class SimpleConstantUninliningTest
{
	@SuppressWarnings("unused")
	private static class Methods
	{
		private static void byteConsumer(byte test) {}

		private static void intConsumer(int test) {}
		
		private static void shortConsumer(short test) {}
		
		private static void charConsumer(char test) {}

		private static void longConsumer(long test) {}

		private static void floatConsumer(float test) {}

		private static void doubleConsumer(double test) {}

		private static void stringConsumer(String test) {}
	}

	@SuppressWarnings("unused")
	private static class Constants
	{
		public static final byte BYTE_CONST_M1 = -1,
								 BYTE_CONST_0 = 0,
								 BYTE_CONST_1 = 1,
								 BYTE_CONST_2 = 2,
								 BYTE_CONST_3 = 3,
								 BYTE_CONST_4 = 4,
								 BYTE_CONST_5 = 5,
								 BYTE_CONST = 117;

		public static final short SHORT_CONST_M1 = -1,
								  SHORT_CONST_0 = 0,
								  SHORT_CONST_1 = 1,
								  SHORT_CONST_2 = 2,
								  SHORT_CONST_3 = 3,
								  SHORT_CONST_4 = 4,
								  SHORT_CONST_5 = 5,
								  SHORT_CONST = 257;
		
		public static final char CHAR_CONST_0 = '\0',
			                     CHAR_CONST_1 = '\1',
			                     CHAR_CONST_2 = '\2',
			                     CHAR_CONST_3 = '\3',
			                     CHAR_CONST_4 = '\4',
			                     CHAR_CONST_5 = '\5',
			                     CHAR_CONST = '\257';
		
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
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("knownBytesProvider")
	public void testKnownByteConstantsParameter(Byte constant, String constantName)
	{	
		testKnownConstantParameter(constant, constantName, "byteConsumer", "(B)V");
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("knownBytesProvider")
	public void testKnownByteConstantsReturn(Byte constant, String constantName)
	{	
		testKnownConstantReturn(constant, constantName);
	}
	
	private static Stream<Arguments> knownBytesProvider()
	{
		return Stream.of
		(
			Arguments.of(Constants.BYTE_CONST_M1, "BYTE_CONST_M1"),
			Arguments.of(Constants.BYTE_CONST_0, "BYTE_CONST_0"),
			Arguments.of(Constants.BYTE_CONST_1, "BYTE_CONST_1"),
			Arguments.of(Constants.BYTE_CONST_2, "BYTE_CONST_2"),
			Arguments.of(Constants.BYTE_CONST_3, "BYTE_CONST_3"),
			Arguments.of(Constants.BYTE_CONST_4, "BYTE_CONST_4"),
			Arguments.of(Constants.BYTE_CONST_5, "BYTE_CONST_5")
		);
	}
	
	@ParameterizedTest(name = "{0}")
	@ValueSource(bytes = {8, 13, 42, -1, -7, -23})
	public void testUnknownByteConstantsParameter(Byte constant)
	{
		testUnknownConstantParameter(constant, "byteConsumer", "(B)V");
	}
	
	@ParameterizedTest(name = "{0}")
	@ValueSource(bytes = {8, 13, 42, -1, -7, -23})
	public void testUnknownByteConstantsReturn(Byte constant)
	{
		testUnknownConstantReturn(constant);
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("knownShortsProvider")
	public void testKnownShortConstantsParameter(Short constant, String constantName)
	{	
		testKnownConstantParameter(constant, constantName, "shortConsumer", "(S)V");
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("knownShortsProvider")
	public void testKnownShortConstantsReturn(Short constant, String constantName)
	{	
		testKnownConstantReturn(constant, constantName);
	}
	
	private static Stream<Arguments> knownShortsProvider()
	{
		return Stream.of
		(
			Arguments.of(Constants.SHORT_CONST_M1, "SHORT_CONST_M1"),
			Arguments.of(Constants.SHORT_CONST_0, "SHORT_CONST_0"),
			Arguments.of(Constants.SHORT_CONST_1, "SHORT_CONST_1"),
			Arguments.of(Constants.SHORT_CONST_2, "SHORT_CONST_2"),
			Arguments.of(Constants.SHORT_CONST_3, "SHORT_CONST_3"),
			Arguments.of(Constants.SHORT_CONST_4, "SHORT_CONST_4"),
			Arguments.of(Constants.SHORT_CONST_5, "SHORT_CONST_5")
		);
	}
	
	@ParameterizedTest(name = "{0}")
	@ValueSource(shorts = {8, 13, 42, -1, -7, -23})
	public void testUnknownShortConstantsParameter(Short constant)
	{
		testUnknownConstantParameter(constant, "shortConsumer", "(S)V");
	}
	
	@ParameterizedTest(name = "{0}")
	@ValueSource(shorts = {8, 13, 42, -1, -7, -23})
	public void testUnknownShortConstantsReturn(Short constant)
	{
		testUnknownConstantReturn(constant);
	}
	
    @ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("knownCharactersProvider")
	public void testKnownCharacterConstantsParameter(Character constant, String constantName)
	{	
		testKnownConstantParameter(constant, constantName, "charConsumer", "(C)V");
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("knownCharactersProvider")
	public void testKnownCharacterConstantsReturn(Character constant, String constantName)
	{	
		testKnownConstantReturn(constant, constantName);
	}
	
	private static Stream<Arguments> knownCharactersProvider()
	{
		return Stream.of
		(
			Arguments.of(Constants.CHAR_CONST_0, "CHAR_CONST_0"),
			Arguments.of(Constants.CHAR_CONST_1, "CHAR_CONST_1"),
			Arguments.of(Constants.CHAR_CONST_2, "CHAR_CONST_2"),
			Arguments.of(Constants.CHAR_CONST_3, "CHAR_CONST_3"),
			Arguments.of(Constants.CHAR_CONST_4, "CHAR_CONST_4"),
			Arguments.of(Constants.CHAR_CONST_5, "CHAR_CONST_5")
		);
	}
	
	@ParameterizedTest(name = "{0}")
	@ValueSource(chars = {'b', '#', ':', 'E', '$', '=', 's', '?', '/', '\\'})
	public void testUnknownCharacterConstantsParameter(Character constant)
	{
		testUnknownConstantParameter(constant, "charConsumer", "(C)V");
	}
	
	@ParameterizedTest(name = "{0}")
	@ValueSource(chars = {'b', '#', ':', 'E', '$', '=', 's', '?', '/', '\\'})
	public void testUnknownCharacterConstantsReturn(Character constant)
	{
		testUnknownConstantReturn(constant);
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
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		MockMethod mock = classResolver.mock(TestUtils.mockInvokeStatic(Methods.class, constantConsumerName, constantConsumerDescriptor, 1));
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.simpleConstantGroup("test")
					.define(fieldClass, fieldName)
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		uninliner.transformMethod(mock.getOwner(), mock.getName(), mock.getDescriptor());
	}

	private void testKnownConstantParameter(Object constant, String expectedConstant, String constantConsumerName, String constantConsumerDescriptor)
	{
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.simpleConstantGroup("test")
					.define(Constants.class, expectedConstant)
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();

		LiteralType literalType = LiteralType.from(constant.getClass());
		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		MockMethod mockInvocation = classResolver.mock(
			TestUtils.mockInvokeStatic(Methods.class, constantConsumerName, constantConsumerDescriptor, constant));
		int invocationInsnIndex = 1;
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, constant, mockInvocation, 
				invocationInsnIndex);
		uninliner.transformMethod(mockInvocation.getOwner(), mockInvocation.getName(), mockInvocation.getDescriptor());
		ASMAssertions.assertReadsField(mockInvocation.getInstructions().get(invocationInsnIndex - 1), Constants.class, expectedConstant, 
				literalType.getTypeDescriptor());
	}

	private void testKnownConstantReturn(Object constant, String expectedConstant)
	{
		LiteralType literalType = LiteralType.from(constant.getClass());
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		MockMethod mock = MethodMocker.mock(literalType.getPrimitiveClass(), mv -> 
		{
			literalType.appendLiteralPushInsn(mv, constant);
			literalType.appendReturnInsn(mv);
		});
		MockMethod mockMethod = classResolver.mock(mock);
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.simpleConstantGroup("test")
					.define(Constants.class, expectedConstant)
				.add()
				.targetMethod(mock.getMockClass().name, mockMethod.getName(), mockMethod.getDescriptor())
					.remapReturn("test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));

		int returnInsnIndex = 1;
		ASMAssertions.assertIsLiteral(mockMethod.getInstructions().get(returnInsnIndex - 1), constant);
		ASMAssertions.assertOpcode(mockMethod.getInstructions().get(returnInsnIndex), literalType.getReturnOpcode());
		uninliner.transformMethod(mockMethod.getOwner(), mockMethod.getName(), mockMethod.getDescriptor());
		ASMAssertions.assertReadsField(mockMethod.getInstructions().get(returnInsnIndex - 1), Constants.class, expectedConstant, 
				literalType.getTypeDescriptor());
	}
	
	private void testUnknownConstantParameter(Object constant, String constantConsumerName, String constantConsumerDescriptor)
	{
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.simpleConstantGroup("test")
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		MockMethod mockInvocation = classResolver.mock(
			TestUtils.mockInvokeStatic(Methods.class, constantConsumerName, constantConsumerDescriptor, constant));
		int invocationInsnIndex = 1;
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, constant, mockInvocation, 
				invocationInsnIndex);
		uninliner.transformMethod(mockInvocation.getOwner(), mockInvocation.getName(), mockInvocation.getDescriptor());
		//Should be unchanged, so this should still pass
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, constant, mockInvocation, 
				invocationInsnIndex);
	}
	
	private void testUnknownConstantReturn(Object constant)
	{
		LiteralType literalType = LiteralType.from(constant.getClass());
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		MockMethod mockMethod = classResolver.mock(MethodMocker.mock(literalType.getPrimitiveClass(), mv -> 
		{
			literalType.appendLiteralPushInsn(mv, constant);
			literalType.appendReturnInsn(mv);
		}));
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.simpleConstantGroup("test")
				.add()
				.targetMethod(mockMethod.getOwner(), mockMethod.getName(), mockMethod.getDescriptor())
					.remapReturn("test")
				.add()
				.build();
		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		
		int returnInsnIndex = 1;
		ASMAssertions.assertIsLiteral(mockMethod.getInstructions().get(returnInsnIndex - 1), constant);
		ASMAssertions.assertOpcode(mockMethod.getInstructions().get(returnInsnIndex), literalType.getReturnOpcode());
		uninliner.transformMethod(mockMethod.getOwner(), mockMethod.getName(), mockMethod.getDescriptor());
		//Should be unchanged, so this should still pass
		ASMAssertions.assertIsLiteral(mockMethod.getInstructions().get(returnInsnIndex - 1), constant);
		ASMAssertions.assertOpcode(mockMethod.getInstructions().get(returnInsnIndex), literalType.getReturnOpcode());
	}

	private void checkMockInvocationStructure(String constantConsumerName,
			String constantConsumerDescriptor, Object expectedLiteralValue,
			MockMethod mockInvocation, int invocationInsnIndex)
	{
		int expectedInstructionCount = 3;
		assertEquals(expectedInstructionCount, mockInvocation.getInstructions().size(), 
				String.format("Expected %d instructions, found %d", expectedInstructionCount, mockInvocation.getInstructions().size()));
		ASMAssertions.assertIsLiteral(mockInvocation.getInstructions().get(invocationInsnIndex - 1), expectedLiteralValue);
		ASMAssertions.assertInvokesMethod(mockInvocation.getInstructions().get(invocationInsnIndex), Methods.class, 
				constantConsumerName, constantConsumerDescriptor);
		ASMAssertions.assertOpcode(mockInvocation.getInstructions().get(invocationInsnIndex + 1), RETURN);
	}
}

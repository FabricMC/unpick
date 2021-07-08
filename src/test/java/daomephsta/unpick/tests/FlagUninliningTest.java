package daomephsta.unpick.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.ListIterator;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.impl.IntegerType;
import daomephsta.unpick.impl.constantresolvers.BytecodeAnalysisConstantResolver;
import daomephsta.unpick.tests.lib.ASMAssertions;
import daomephsta.unpick.tests.lib.MethodMocker;
import daomephsta.unpick.tests.lib.MethodMocker.MockMethod;
import daomephsta.unpick.tests.lib.MockConstantMapper;
import daomephsta.unpick.tests.lib.TestUtils;

public class FlagUninliningTest
{
	@SuppressWarnings("unused")
	private static class Constants
	{
		public static final byte BYTE_FLAG_BIT_0 = 1 << 0,
								 BYTE_FLAG_BIT_1 = 1 << 1,
								 BYTE_FLAG_BIT_2 = 1 << 2,
								 BYTE_FLAG_BIT_3 = 1 << 3;
		
		public static final short SHORT_FLAG_BIT_0 = 1 << 0,
								  SHORT_FLAG_BIT_1 = 1 << 1,
								  SHORT_FLAG_BIT_2 = 1 << 2,
								  SHORT_FLAG_BIT_3 = 1 << 3;
		
		public static final int INT_FLAG_BIT_0 = 1 << 0,
								INT_FLAG_BIT_1 = 1 << 1,
								INT_FLAG_BIT_2 = 1 << 2,
								INT_FLAG_BIT_3 = 1 << 3;
		
		public static final long LONG_FLAG_BIT_0 = 1 << 0,
								 LONG_FLAG_BIT_1 = 1 << 1,
								 LONG_FLAG_BIT_2 = 1 << 2,
								 LONG_FLAG_BIT_3 = 1 << 3;
	}
	
	@SuppressWarnings("unused")
	private static class Methods
	{
		private static void byteConsumer(byte test) {}
		
		private static void shortConsumer(short test) {}

		private static void intConsumer(int test) {}

		private static void longConsumer(long test) {}
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("byteFlagsProvider")
	public void testKnownByteFlagsReturn(Byte testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testKnownFlagsReturn(testConstant, expectedConstantCombination, constantNames);
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("byteFlagsProvider")
	public void testKnownByteFlagsParameter(Byte testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testKnownFlagsParameter(testConstant, expectedConstantCombination, constantNames, "byteConsumer", "(B)V");
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("shortFlagsProvider")
	public void testKnownShortFlagsReturn(Short testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testKnownFlagsReturn(testConstant, expectedConstantCombination, constantNames);
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("shortFlagsProvider")
	public void testKnownShortFlagsParameter(Short testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testKnownFlagsParameter(testConstant, expectedConstantCombination, constantNames, "shortConsumer", "(S)V");
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("intFlagsProvider")
	public void testKnownIntFlagsReturn(Integer testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testKnownFlagsReturn(testConstant, expectedConstantCombination, constantNames);
	}
	
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("intFlagsProvider")
	public void testKnownIntFlagsParameter(Integer testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testKnownFlagsParameter(testConstant, expectedConstantCombination, constantNames, "intConsumer", "(I)V");
	}
	
	@ParameterizedTest(name = "{0}L -> {1}")
	@MethodSource("longFlagsProvider")
	public void testKnownLongFlagsParameter(Long testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testKnownFlagsParameter(testConstant, expectedConstantCombination, constantNames, "longConsumer", "(J)V");
	}
	
	@ParameterizedTest(name = "{0}L -> {1}")
	@MethodSource("longFlagsProvider")
	public void testKnownLongFlagsReturn(Long testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testKnownFlagsReturn(testConstant, expectedConstantCombination, constantNames);
	}

	private void testKnownFlagsParameter(Number testConstant, String[] expectedConstantCombination, String[] constantNames, String constantConsumerName, String constantConsumerDescriptor)
	{
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.flagConstantGroup("test")
					.defineAll(Constants.class, constantNames)
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();
		
		IntegerType integerType = IntegerType.from(testConstant);
		
		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		
		MockMethod mockMethod = classResolver.mock(
			TestUtils.mockInvokeStatic(Methods.class, constantConsumerName, constantConsumerDescriptor, testConstant));
		int invocationInsnIndex = 1;
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, testConstant, mockMethod, invocationInsnIndex);
		uninliner.transformMethod(mockMethod.getOwner(), mockMethod.getName(), mockMethod.getDescriptor());
		int minimumInsnCount = 2 * (expectedConstantCombination.length - 1) + 1;
		assertTrue(mockMethod.getInstructions().size() >=  minimumInsnCount, 
				String.format("Expected at least %d instructions, found %d", minimumInsnCount, mockMethod.getInstructions().size()));
		invocationInsnIndex += minimumInsnCount - 1;
		ASMAssertions.assertInvokesMethod(mockMethod.getInstructions().get(invocationInsnIndex), Methods.class, 
				constantConsumerName, constantConsumerDescriptor);
		ASMAssertions.assertReadsField(mockMethod.getInstructions().get(0), Constants.class, 
				expectedConstantCombination[0], integerType.getTypeDescriptor());
		for (int j = 1; j < expectedConstantCombination.length; j += 2)
		{
			ASMAssertions.assertReadsField(mockMethod.getInstructions().get(j), Constants.class, 
					expectedConstantCombination[j], integerType.getTypeDescriptor());
			ASMAssertions.assertOpcode(mockMethod.getInstructions().get(j + 1), integerType.getOrOpcode());
		}
	}

	private void testKnownFlagsReturn(Number testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		IntegerType integerType = IntegerType.from(testConstant);
		MockMethod mock = classResolver.mock(MethodMocker.mock(integerType.getPrimitiveClass(), mv -> 
		{
			integerType.appendLiteralPushInsn(mv, testConstant.longValue());
			integerType.appendReturnInsn(mv);
		}));
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.flagConstantGroup("test")
					.defineAll(Constants.class, constantNames)
				.add()
				.targetMethod(mock.getOwner(), mock.getName(), mock.getDescriptor())
					.remapReturn("test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		
		uninliner.transformMethod(mock.getOwner(), mock.getName(), mock.getDescriptor());
		int minimumInsnCount = 2 * (expectedConstantCombination.length - 1) + 1;
		assertTrue(mock.getInstructions().size() >=  minimumInsnCount, 
				String.format("Expected at least %d instructions, found %d", minimumInsnCount, mock.getInstructions().size()));
		ASMAssertions.assertOpcode(mock.getInstructions().get(mock.getInstructions().size() - 1), integerType.getReturnOpcode());
		ASMAssertions.assertReadsField(mock.getInstructions().get(0), Constants.class, 
				expectedConstantCombination[0], integerType.getTypeDescriptor());
		for (int j = 1; j < expectedConstantCombination.length; j += 2)
		{
			ASMAssertions.assertReadsField(mock.getInstructions().get(j), Constants.class, 
					expectedConstantCombination[j], integerType.getTypeDescriptor());
			ASMAssertions.assertOpcode(mock.getInstructions().get(j + 1), integerType.getOrOpcode());
		}
	}

	@ParameterizedTest(name = "~{0} -> {1}")
	@MethodSource("byteFlagsProvider")
	public void testNegatedByteFlagsParameter(Byte testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testNegatedFlagsParameter(testConstant, expectedConstantCombination, constantNames, "byteConsumer", "(B)V");
	}
	
	@ParameterizedTest(name = "~{0} -> {1}")
	@MethodSource("byteFlagsProvider")
	public void testNegatedByteFlagsReturn(Byte testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testNegatedFlagsReturn(testConstant, expectedConstantCombination, constantNames);
	}

	@ParameterizedTest(name = "~{0} -> {1}")
	@MethodSource("shortFlagsProvider")
	public void testNegatedShortFlagsParameter(Short testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testNegatedFlagsParameter(testConstant, expectedConstantCombination, constantNames, "shortConsumer", "(B)V");
	}
	
	@ParameterizedTest(name = "~{0} -> {1}")
	@MethodSource("shortFlagsProvider")
	public void testNegatedShortFlagsReturn(Short testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testNegatedFlagsReturn(testConstant, expectedConstantCombination, constantNames);
	}
	
	@ParameterizedTest(name = "~{0} -> {1}")
	@MethodSource("intFlagsProvider")
	public void testNegatedIntFlagsParameter(Integer testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testNegatedFlagsParameter(testConstant, expectedConstantCombination, constantNames, "intConsumer", "(I)V");
	}
	
	@ParameterizedTest(name = "~{0} -> {1}")
	@MethodSource("intFlagsProvider")
	public void testNegatedIntFlagsReturn(Integer testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testNegatedFlagsReturn(testConstant, expectedConstantCombination, constantNames);
	}
	
	@ParameterizedTest(name = "~{0}L -> {1}")
	@MethodSource("longFlagsProvider")
	public void testNegatedLongFlagsParameter(Long testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testNegatedFlagsParameter(testConstant, expectedConstantCombination, constantNames, "longConsumer", "(J)V");
	}
	
	@ParameterizedTest(name = "~{0}L -> {1}")
	@MethodSource("longFlagsProvider")
	public void testNegatedLongFlagsReturn(Long testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		testNegatedFlagsReturn(testConstant, expectedConstantCombination, constantNames);
	}
	
	private void testNegatedFlagsParameter(Number testConstant, String[] expectedConstantCombination, String[] constantNames, String consumerName, String consumerDescriptor)
	{		
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.flagConstantGroup("test")
				.defineAll(Constants.class, constantNames)
				.add()
				.targetMethod(Methods.class, consumerName, consumerDescriptor)
				.remapParameter(0, "test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		IntegerType integerType = IntegerType.from(testConstant);

		MockMethod mockMethod = classResolver.mock(MethodMocker.mock(void.class, mv -> 
		{
			mv.visitFieldInsn(Opcodes.GETSTATIC, "Foo", "bar", integerType.getTypeDescriptor());
			integerType.appendLiteralPushInsn(mv, ~testConstant.longValue());
			integerType.appendAndInsn(mv);
			mv.visitMethodInsn(INVOKESTATIC, Methods.class.getName().replace('.', '/'), consumerName, consumerDescriptor, false);
			mv.visitInsn(RETURN);
		}));
		uninliner.transformMethod(mockMethod.getOwner(), mockMethod.getName(), mockMethod.getDescriptor());
		ListIterator<AbstractInsnNode> instructions = mockMethod.getInstructions().iterator(0);
		ASMAssertions.assertReadsField(instructions.next(), "Foo", "bar", integerType.getTypeDescriptor());

		ASMAssertions.assertReadsField(instructions.next(), Constants.class, 
				expectedConstantCombination[0], integerType.getTypeDescriptor());
		for (int j = 1; j < expectedConstantCombination.length; j += 2)
		{
			ASMAssertions.assertReadsField(instructions.next(), Constants.class, 
					expectedConstantCombination[j], integerType.getTypeDescriptor());
			ASMAssertions.assertOpcode(instructions.next(), integerType.getOrOpcode());
		}
	}
	
	private void testNegatedFlagsReturn(Number testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		IntegerType integerType = IntegerType.from(testConstant);
		MockMethod mock = classResolver.mock(MethodMocker.mock(integerType.getPrimitiveClass(), mv -> 
		{
			mv.visitFieldInsn(Opcodes.GETSTATIC, "Foo", "bar", integerType.getTypeDescriptor());
			integerType.appendLiteralPushInsn(mv, ~testConstant.longValue());
			integerType.appendAndInsn(mv);
			integerType.appendReturnInsn(mv);
		}));
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.flagConstantGroup("test")
					.defineAll(Constants.class, constantNames)
				.add()
				.targetMethod(mock.getOwner(), mock.getName(), mock.getDescriptor())
					.remapReturn("test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));
		
		uninliner.transformMethod(mock.getOwner(), mock.getName(), mock.getDescriptor());
		ListIterator<AbstractInsnNode> instructions = mock.getInstructions().iterator(0);
		ASMAssertions.assertReadsField(instructions.next(), "Foo", "bar", integerType.getTypeDescriptor());

		ASMAssertions.assertReadsField(instructions.next(), Constants.class, 
				expectedConstantCombination[0], integerType.getTypeDescriptor());
		for (int j = 1; j < expectedConstantCombination.length; j += 2)
		{
			ASMAssertions.assertReadsField(instructions.next(), Constants.class, 
					expectedConstantCombination[j], integerType.getTypeDescriptor());
			ASMAssertions.assertOpcode(instructions.next(), integerType.getOrOpcode());
		}
	}
	
	@ParameterizedTest(name = "{0} -> {0}")
	@ValueSource(bytes = {0b0000, 0b100000, 0b01000, 0b11000})
	public void testUnknownByteFlagsParameter(Byte testConstant)
	{
		testUnknownFlagsParameter(testConstant, "byteConsumer", "(B)V");
	}
	@ParameterizedTest(name = "{0} -> {0}")
	@ValueSource(bytes = {0b0000, 0b100000, 0b01000, 0b11000})
	public void testUnknownByteFlagsReturn(Byte testConstant)
	{
		testUnknownFlagsReturn(testConstant);
	}

	@ParameterizedTest(name = "{0} -> {0}")
	@ValueSource(shorts = {0b0000, 0b100000, 0b01000, 0b11000})
	public void testUnknownShortFlagsParameter(Short testConstant)
	{
		testUnknownFlagsParameter(testConstant, "shortConsumer", "(S)V");
	}
	@ParameterizedTest(name = "{0} -> {0}")
	@ValueSource(shorts = {0b0000, 0b100000, 0b01000, 0b11000})
	public void testUnknownShortFlagsReturn(Short testConstant)
	{
		testUnknownFlagsReturn(testConstant);
	}	
	
	@ParameterizedTest(name = "{0} -> {0}")
	@ValueSource(ints = {0b0000, 0b100000, 0b01000, 0b11000})
	public void testUnknownIntFlagsParameter(Integer testConstant)
	{
		testUnknownFlagsParameter(testConstant, "intConsumer", "(I)V");
	}
	
	@ParameterizedTest(name = "{0} -> {0}")
	@ValueSource(ints = {0b0000, 0b100000, 0b01000, 0b11000})
	public void testUnknownIntFlagsReturn(Integer testConstant)
	{
		testUnknownFlagsReturn(testConstant);
	}
	
	@ParameterizedTest(name = "{0}L -> {0}L")
	@ValueSource(longs = {0b0000L, 0b100000L, 0b01000L, 0b11000L})
	public void testUnknownLongFlagsParameter(Long testConstant)
	{
		testUnknownFlagsParameter(testConstant, "longConsumer", "(J)V");
	}
	@ParameterizedTest(name = "{0}L -> {0}L")
	@ValueSource(longs = {0b0000L, 0b100000L, 0b01000L, 0b11000L})
	public void testUnknownLongFlagsReturn(Long testConstant)
	{
		testUnknownFlagsReturn(testConstant);
	}
	
	private void testUnknownFlagsParameter(Number constant, String constantConsumerName, String constantConsumerDescriptor)
	{
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.flagConstantGroup("test")
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
	
	private void testUnknownFlagsReturn(Number testConstant)
	{
		MethodMockingClassResolver classResolver = new MethodMockingClassResolver();
		IntegerType integerType = IntegerType.from(testConstant);
		MockMethod mock = classResolver.mock(MethodMocker.mock(integerType.getPrimitiveClass(), mv -> 
		{
			integerType.appendLiteralPushInsn(mv, testConstant.longValue());
			integerType.appendReturnInsn(mv);
		}));
		IConstantMapper mapper = MockConstantMapper.builder(classResolver)
				.flagConstantGroup("test")
				.add()
				.targetMethod(mock.getMockClass().name, mock.getName(), mock.getDescriptor())
					.remapReturn("test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(classResolver, mapper, 
			new BytecodeAnalysisConstantResolver(classResolver));

		ASMAssertions.assertIsLiteral(mock.getInstructions().get(0), testConstant);
		ASMAssertions.assertOpcode(mock.getInstructions().get(1), integerType.getReturnOpcode());
		uninliner.transformMethod(mock.getOwner(), mock.getName(), mock.getDescriptor());
		//Should be unchanged, so this should still pass
		ASMAssertions.assertIsLiteral(mock.getInstructions().get(0), testConstant);
		ASMAssertions.assertOpcode(mock.getInstructions().get(1), integerType.getReturnOpcode());
	}
	
	private static Stream<Arguments> byteFlagsProvider()
	{
		String[] constantNames = {"BYTE_FLAG_BIT_0", "BYTE_FLAG_BIT_1", "BYTE_FLAG_BIT_2", "BYTE_FLAG_BIT_3"};
		return Stream.of
		(
			Arguments.of((byte) 0b0100, new String[] {"BYTE_FLAG_BIT_2"}, constantNames),
			Arguments.of((byte) 0b1100, new String[] {"BYTE_FLAG_BIT_2", "BYTE_FLAG_BIT_3"}, constantNames),
			Arguments.of((byte) 0b1010, new String[] {"BYTE_FLAG_BIT_1", "BYTE_FLAG_BIT_3"}, constantNames),
			Arguments.of((byte) 0b0111, new String[] {"BYTE_FLAG_BIT_0", "BYTE_FLAG_BIT_1", "BYTE_FLAG_BIT_2"}, constantNames)
		);	
	}
	
	private static Stream<Arguments> shortFlagsProvider()
	{
		String[] constantNames = {"SHORT_FLAG_BIT_0", "SHORT_FLAG_BIT_1", "SHORT_FLAG_BIT_2", "SHORT_FLAG_BIT_3"};
		return Stream.of
		(
			Arguments.of((short) 0b0100, new String[] {"SHORT_FLAG_BIT_2"}, constantNames),
			Arguments.of((short) 0b1100, new String[] {"SHORT_FLAG_BIT_2", "SHORT_FLAG_BIT_3"}, constantNames),
			Arguments.of((short) 0b1010, new String[] {"SHORT_FLAG_BIT_1", "SHORT_FLAG_BIT_3"}, constantNames),
			Arguments.of((short) 0b0111, new String[] {"SHORT_FLAG_BIT_0", "SHORT_FLAG_BIT_1", "SHORT_FLAG_BIT_2"}, constantNames)
		);	
	}
	
	private static Stream<Arguments> intFlagsProvider()
	{
		String[] constantNames = {"INT_FLAG_BIT_0", "INT_FLAG_BIT_1", "INT_FLAG_BIT_2", "INT_FLAG_BIT_3"};
		return Stream.of
		(
			Arguments.of(0b0100, new String[] {"INT_FLAG_BIT_2"}, constantNames),
			Arguments.of(0b1100, new String[] {"INT_FLAG_BIT_2", "INT_FLAG_BIT_3"}, constantNames),
			Arguments.of(0b1010, new String[] {"INT_FLAG_BIT_1", "INT_FLAG_BIT_3"}, constantNames),
			Arguments.of(0b0111, new String[] {"INT_FLAG_BIT_0", "INT_FLAG_BIT_1", "INT_FLAG_BIT_2"}, constantNames)
		);	
	}
	
	private static Stream<Arguments> longFlagsProvider()
	{
		String[] constantNames = {"LONG_FLAG_BIT_0", "LONG_FLAG_BIT_1", "LONG_FLAG_BIT_2", "LONG_FLAG_BIT_3"};
		return Stream.of
		(
			Arguments.of(0b0100L, new String[] {"LONG_FLAG_BIT_2"}, constantNames),
			Arguments.of(0b1100L, new String[] {"LONG_FLAG_BIT_2", "LONG_FLAG_BIT_3"}, constantNames),
			Arguments.of(0b1010L, new String[] {"LONG_FLAG_BIT_1", "LONG_FLAG_BIT_3"}, constantNames),
			Arguments.of(0b0111L, new String[] {"LONG_FLAG_BIT_0", "LONG_FLAG_BIT_1", "LONG_FLAG_BIT_2"}, constantNames)
		);	
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

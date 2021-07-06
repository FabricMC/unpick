package daomephsta.unpick.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import java.io.IOException;
import java.util.ListIterator;
import java.util.stream.Stream;

import daomephsta.unpick.api.IClassResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.impl.IntegerType;
import daomephsta.unpick.impl.constantresolvers.ClasspathConstantResolver;
import daomephsta.unpick.tests.lib.*;
import daomephsta.unpick.tests.lib.MethodMocker.MockMethod;

public class FlagUninliningTest
{
	private static final ClasspathConstantResolver CONSTANT_RESOLVER = new ClasspathConstantResolver();

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
		
		public static final IClassResolver CLASS_RESOLVER = internalName -> {
			try {
				return new ClassReader(internalName);
			} catch (IOException e) {
				throw new IClassResolver.ClassResolutionException(e);
			}
		};
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
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER, CONSTANT_RESOLVER)
				.flagConstantGroup("test")
					.defineAll(Constants.class, constantNames)
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();
		
		IntegerType integerType = IntegerType.from(testConstant);
		ConstantUninliner uninliner = new ConstantUninliner(mapper, CONSTANT_RESOLVER);
		
		MockMethod mockMethod = TestUtils.mockInvokeStatic(Methods.class, 
				constantConsumerName, constantConsumerDescriptor, testConstant);
		MethodNode mockInvocation = mockMethod.getMockMethod();
		int invocationInsnIndex = 1;
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, testConstant, mockInvocation, invocationInsnIndex);
		uninliner.transformMethod(MethodMocker.CLASS_NAME, mockInvocation);
		int minimumInsnCount = 2 * (expectedConstantCombination.length - 1) + 1;
		assertTrue(mockInvocation.instructions.size() >=  minimumInsnCount, 
				String.format("Expected at least %d instructions, found %d", minimumInsnCount, mockInvocation.instructions.size()));
		invocationInsnIndex += minimumInsnCount - 1;
		ASMAssertions.assertInvokesMethod(mockInvocation.instructions.get(invocationInsnIndex), Methods.class, 
				constantConsumerName, constantConsumerDescriptor);
		ASMAssertions.assertReadsField(mockInvocation.instructions.get(0), Constants.class, 
				expectedConstantCombination[0], integerType.getTypeDescriptor());
		for (int j = 1; j < expectedConstantCombination.length; j += 2)
		{
			ASMAssertions.assertReadsField(mockInvocation.instructions.get(j), Constants.class, 
					expectedConstantCombination[j], integerType.getTypeDescriptor());
			ASMAssertions.assertOpcode(mockInvocation.instructions.get(j + 1), integerType.getOrOpcode());
		}
	}

	private void testKnownFlagsReturn(Number testConstant, String[] expectedConstantCombination, String[] constantNames)
	{
		IntegerType integerType = IntegerType.from(testConstant);
		MockMethod mock = MethodMocker.mock(integerType.getPrimitiveClass(), mv -> 
		{
			integerType.appendLiteralPushInsn(mv, testConstant.longValue());
			integerType.appendReturnInsn(mv);
		});
		MethodNode mockMethod = mock.getMockMethod();
		
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER, CONSTANT_RESOLVER)
				.flagConstantGroup("test")
					.defineAll(Constants.class, constantNames)
				.add()
				.targetMethod(mock.getMockClass().name, mockMethod.name, mockMethod.desc)
					.remapReturn("test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(mapper, CONSTANT_RESOLVER);
		
		uninliner.transformMethod(mock.getMockClass().name, mockMethod);
		int minimumInsnCount = 2 * (expectedConstantCombination.length - 1) + 1;
		assertTrue(mockMethod.instructions.size() >=  minimumInsnCount, 
				String.format("Expected at least %d instructions, found %d", minimumInsnCount, mockMethod.instructions.size()));
		ASMAssertions.assertOpcode(mockMethod.instructions.get(mockMethod.instructions.size() - 1), integerType.getReturnOpcode());
		ASMAssertions.assertReadsField(mockMethod.instructions.get(0), Constants.class, 
				expectedConstantCombination[0], integerType.getTypeDescriptor());
		for (int j = 1; j < expectedConstantCombination.length; j += 2)
		{
			ASMAssertions.assertReadsField(mockMethod.instructions.get(j), Constants.class, 
					expectedConstantCombination[j], integerType.getTypeDescriptor());
			ASMAssertions.assertOpcode(mockMethod.instructions.get(j + 1), integerType.getOrOpcode());
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
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER, CONSTANT_RESOLVER)
				.flagConstantGroup("test")
				.defineAll(Constants.class, constantNames)
				.add()
				.targetMethod(Methods.class, consumerName, consumerDescriptor)
				.remapParameter(0, "test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(mapper, CONSTANT_RESOLVER);
		IntegerType integerType = IntegerType.from(testConstant);

		MockMethod mockMethod = MethodMocker.mock(void.class, mv -> 
		{
			mv.visitFieldInsn(Opcodes.GETSTATIC, "Foo", "bar", integerType.getTypeDescriptor());
			integerType.appendLiteralPushInsn(mv, ~testConstant.longValue());
			integerType.appendAndInsn(mv);
			mv.visitMethodInsn(INVOKESTATIC, Methods.class.getName().replace('.', '/'), consumerName, consumerDescriptor, false);
			mv.visitInsn(RETURN);
		});
		MethodNode mockInvocation = mockMethod.getMockMethod();
		uninliner.transformMethod(MethodMocker.CLASS_NAME, mockInvocation);
		ListIterator<AbstractInsnNode> instructions = mockInvocation.instructions.iterator(0);
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
		IntegerType integerType = IntegerType.from(testConstant);
		MockMethod mock = MethodMocker.mock(integerType.getPrimitiveClass(), mv -> 
		{
			mv.visitFieldInsn(Opcodes.GETSTATIC, "Foo", "bar", integerType.getTypeDescriptor());
			integerType.appendLiteralPushInsn(mv, ~testConstant.longValue());
			integerType.appendAndInsn(mv);
			integerType.appendReturnInsn(mv);
		});
		MethodNode mockMethod = mock.getMockMethod();
		
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER, CONSTANT_RESOLVER)
				.flagConstantGroup("test")
					.defineAll(Constants.class, constantNames)
				.add()
				.targetMethod(mock.getMockClass().name, mockMethod.name, mockMethod.desc)
					.remapReturn("test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(mapper, CONSTANT_RESOLVER);
		
		uninliner.transformMethod(MethodMocker.CLASS_NAME, mockMethod);
		ListIterator<AbstractInsnNode> instructions = mockMethod.instructions.iterator(0);
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
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER, CONSTANT_RESOLVER)
				.flagConstantGroup("test")
				.add()
				.targetMethod(Methods.class, constantConsumerName, constantConsumerDescriptor)
					.remapParameter(0, "test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(mapper, CONSTANT_RESOLVER);

		MethodNode mockInvocation = TestUtils.mockInvokeStatic(Methods.class, 
				constantConsumerName, constantConsumerDescriptor, constant).getMockMethod();
		int invocationInsnIndex = 1;
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, constant, mockInvocation, 
				invocationInsnIndex);
		uninliner.transformMethod(MethodMocker.CLASS_NAME, mockInvocation);
		//Should be unchanged, so this should still pass
		checkMockInvocationStructure(constantConsumerName, constantConsumerDescriptor, constant, mockInvocation, 
				invocationInsnIndex);
	}
	
	private void testUnknownFlagsReturn(Number testConstant)
	{
		IntegerType integerType = IntegerType.from(testConstant);
		MockMethod mock = MethodMocker.mock(integerType.getPrimitiveClass(), mv -> 
		{
			integerType.appendLiteralPushInsn(mv, testConstant.longValue());
			integerType.appendReturnInsn(mv);
		});
		MethodNode mockMethod = mock.getMockMethod();
		
		IConstantMapper mapper = MockConstantMapper.builder(Constants.CLASS_RESOLVER, CONSTANT_RESOLVER)
				.flagConstantGroup("test")
				.add()
				.targetMethod(mock.getMockClass().name, mockMethod.name, mockMethod.desc)
					.remapReturn("test")
				.add()
				.build();

		ConstantUninliner uninliner = new ConstantUninliner(mapper, CONSTANT_RESOLVER);

		MethodNode mockInvocation = mockMethod;
		ASMAssertions.assertIsLiteral(mockInvocation.instructions.get(0), testConstant);
		ASMAssertions.assertOpcode(mockInvocation.instructions.get(1), integerType.getReturnOpcode());
		uninliner.transformMethod(MethodMocker.CLASS_NAME, mockInvocation);
		//Should be unchanged, so this should still pass
		ASMAssertions.assertIsLiteral(mockInvocation.instructions.get(0), testConstant);
		ASMAssertions.assertOpcode(mockInvocation.instructions.get(1), integerType.getReturnOpcode());
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
			MethodNode mockInvocation, int invocationInsnIndex)
	{
		int expectedInstructionCount = 3;
		assertEquals(expectedInstructionCount, mockInvocation.instructions.size(), 
				String.format("Expected %d instructions, found %d", expectedInstructionCount, mockInvocation.instructions.size()));
		ASMAssertions.assertIsLiteral(mockInvocation.instructions.get(invocationInsnIndex - 1), expectedLiteralValue);
		ASMAssertions.assertInvokesMethod(mockInvocation.instructions.get(invocationInsnIndex), Methods.class, 
				constantConsumerName, constantConsumerDescriptor);
		ASMAssertions.assertOpcode(mockInvocation.instructions.get(invocationInsnIndex + 1), RETURN);
	}
}

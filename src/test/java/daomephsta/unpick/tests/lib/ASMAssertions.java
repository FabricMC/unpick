package daomephsta.unpick.tests.lib;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;
import org.opentest4j.AssertionFailedError;

public class ASMAssertions {
	public static void assertClassEquals(ClassNode expectedClassNode, ClassNode actualClassNode) {
		try {
			assertClassEqualsInner(expectedClassNode, actualClassNode);
		} catch (AssertionFailedError e) {
			assertionFailure()
					.expected(classToString(expectedClassNode))
					.actual(classToString(actualClassNode))
					.cause(e)
					.buildAndThrow();
		}
	}

	private static String classToString(ClassNode classNode) {
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			TraceClassVisitor cv = new TraceClassVisitor(pw);
			classNode.accept(cv);
		}
		return sw.toString();
	}

	private static void assertClassEqualsInner(ClassNode expectedClassNode, ClassNode actualClassNode) {
		assertEquals(expectedClassNode.version, actualClassNode.version);
		assertEquals(expectedClassNode.access, actualClassNode.access);
		assertEquals(expectedClassNode.name, actualClassNode.name);
		assertEquals(expectedClassNode.signature, actualClassNode.signature);
		assertEquals(expectedClassNode.superName, actualClassNode.superName);
		assertEquals(expectedClassNode.interfaces, actualClassNode.interfaces);

		int expectedNumFields = expectedClassNode.fields == null ? 0 : expectedClassNode.fields.size();
		int actualNumFields = actualClassNode.fields == null ? 0 : actualClassNode.fields.size();
		assertEquals(expectedNumFields, actualNumFields, "Number of fields");
		for (int i = 0; i < expectedNumFields; i++) {
			assertFieldEqualsInner(expectedClassNode.fields.get(i), actualClassNode.fields.get(i));
		}

		int expectedNumMethods = expectedClassNode.methods == null ? 0 : expectedClassNode.methods.size();
		int actualNumMethods = actualClassNode.methods == null ? 0 : actualClassNode.methods.size();
		assertEquals(expectedNumMethods, actualNumMethods, "Number of methods");
		for (int i = 0; i < expectedNumMethods; i++) {
			assertMethodEqualsInner(expectedClassNode.methods.get(i), actualClassNode.methods.get(i));
		}
	}

	private static void assertFieldEqualsInner(FieldNode expectedFieldNode, FieldNode actualFieldNode) {
		assertEquals(expectedFieldNode.access, actualFieldNode.access);
		assertEquals(expectedFieldNode.name, actualFieldNode.name);
		assertEquals(expectedFieldNode.desc, actualFieldNode.desc);
		assertEquals(expectedFieldNode.signature, actualFieldNode.signature);
		assertEquals(expectedFieldNode.value, actualFieldNode.value);
	}

	private static void assertMethodEqualsInner(MethodNode expectedMethodNode, MethodNode actualMethodNode) {
		assertEquals(expectedMethodNode.access, actualMethodNode.access);
		assertEquals(expectedMethodNode.name, actualMethodNode.name);
		assertEquals(expectedMethodNode.desc, actualMethodNode.desc);
		assertEquals(expectedMethodNode.signature, actualMethodNode.signature);
		assertEquals(expectedMethodNode.exceptions, actualMethodNode.exceptions);
		assertEquals(expectedMethodNode.annotationDefault, actualMethodNode.annotationDefault);

		Map<LabelNode, Integer> expectedInsnIndexByLabel = new HashMap<>();
		Map<LabelNode, Integer> actualInsnIndexByLabel = new HashMap<>();

		int insnIndex = 0;
		for (AbstractInsnNode insn : expectedMethodNode.instructions) {
			if (insn.getOpcode() >= 0) {
				insnIndex++;
			} else if (insn.getType() == AbstractInsnNode.LABEL) {
				expectedInsnIndexByLabel.put((LabelNode) insn, insnIndex++);
			}
		}
		insnIndex = 0;
		for (AbstractInsnNode insn : actualMethodNode.instructions) {
			if (insn.getOpcode() >= 0) {
				insnIndex++;
			} else if (insn.getType() == AbstractInsnNode.LABEL) {
				actualInsnIndexByLabel.put((LabelNode) insn, insnIndex++);
			}
		}

		int expectedIndex = 0;
		int actualIndex = 0;
		while (true) {
			AbstractInsnNode expectedInsn = null;
			while (expectedIndex < expectedMethodNode.instructions.size() && (expectedInsn = expectedMethodNode.instructions.get(expectedIndex)).getOpcode() < 0) {
				expectedIndex++;
			}

			AbstractInsnNode actualInsn = null;
			while (actualIndex < actualMethodNode.instructions.size() && (actualInsn = actualMethodNode.instructions.get(actualIndex)).getOpcode() < 0) {
				actualIndex++;
			}

			assertEquals(expectedIndex >= expectedMethodNode.instructions.size(), actualIndex >= expectedMethodNode.instructions.size(), "Mismatching number of instructions");
			if (expectedIndex >= expectedMethodNode.instructions.size()) {
				break;
			}
			assert expectedInsn != null && actualInsn != null; // should never happen

			assertInstructionEquals(expectedInsn, actualInsn, expectedInsnIndexByLabel, actualInsnIndexByLabel);

			expectedIndex++;
			actualIndex++;
		}

		int expectedTryCatchBlockCount = expectedMethodNode.tryCatchBlocks == null ? 0 : expectedMethodNode.tryCatchBlocks.size();
		int actualTryCatchBlockCount = actualMethodNode.tryCatchBlocks == null ? 0 : actualMethodNode.tryCatchBlocks.size();
		assertEquals(expectedTryCatchBlockCount, actualTryCatchBlockCount, "Number of try catch blocks");

		for (int i = 0; i < expectedTryCatchBlockCount; i++) {
			TryCatchBlockNode expectedTryCatchBlock = expectedMethodNode.tryCatchBlocks.get(i);
			TryCatchBlockNode actualTryCatchBlock = actualMethodNode.tryCatchBlocks.get(i);
			assertEquals(expectedTryCatchBlock.type, actualTryCatchBlock.type);
			assertEquals(expectedInsnIndexByLabel.get(expectedTryCatchBlock.start), actualInsnIndexByLabel.get(expectedTryCatchBlock.start));
			assertEquals(expectedInsnIndexByLabel.get(expectedTryCatchBlock.end), actualInsnIndexByLabel.get(expectedTryCatchBlock.end));
			assertEquals(expectedInsnIndexByLabel.get(expectedTryCatchBlock.handler), actualInsnIndexByLabel.get(expectedTryCatchBlock.handler));
		}
	}

	private static void assertInstructionEquals(AbstractInsnNode expectedInsn, AbstractInsnNode actualInsn, Map<LabelNode, Integer> expectedInsnIndexByLabel, Map<LabelNode, Integer> actualInsnIndexByLabel) {
		assertEquals(Printer.OPCODES[expectedInsn.getOpcode()], Printer.OPCODES[actualInsn.getOpcode()]);
		switch (expectedInsn.getType()) {
			case AbstractInsnNode.INSN:
				break;
			case AbstractInsnNode.INT_INSN:
				assertEquals(((IntInsnNode) expectedInsn).operand, ((IntInsnNode) actualInsn).operand);
				break;
			case AbstractInsnNode.VAR_INSN:
				assertEquals(((VarInsnNode) expectedInsn).var, ((VarInsnNode) actualInsn).var);
				break;
			case AbstractInsnNode.TYPE_INSN:
				assertEquals(((TypeInsnNode) expectedInsn).desc, ((TypeInsnNode) actualInsn).desc);
				break;
			case AbstractInsnNode.FIELD_INSN:
				FieldInsnNode expectedFieldInsn = (FieldInsnNode) expectedInsn;
				FieldInsnNode actualFieldInsn = (FieldInsnNode) actualInsn;
				assertEquals(expectedFieldInsn.owner, actualFieldInsn.owner);
				assertEquals(expectedFieldInsn.name, actualFieldInsn.name);
				assertEquals(expectedFieldInsn.desc, actualFieldInsn.desc);
				break;
			case AbstractInsnNode.METHOD_INSN:
				MethodInsnNode expectedMethodInsn = (MethodInsnNode) expectedInsn;
				MethodInsnNode actualMethodInsn = (MethodInsnNode) actualInsn;
				assertEquals(expectedMethodInsn.owner, actualMethodInsn.owner);
				assertEquals(expectedMethodInsn.name, actualMethodInsn.name);
				assertEquals(expectedMethodInsn.desc, actualMethodInsn.desc);
				assertEquals(expectedMethodInsn.itf, actualMethodInsn.itf);
				break;
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
				InvokeDynamicInsnNode expectedInvokeDynamicInsn = (InvokeDynamicInsnNode) expectedInsn;
				InvokeDynamicInsnNode actualInvokeDynamicInsn = (InvokeDynamicInsnNode) actualInsn;
				assertEquals(expectedInvokeDynamicInsn.name, actualInvokeDynamicInsn.name);
				assertEquals(expectedInvokeDynamicInsn.desc, actualInvokeDynamicInsn.desc);
				assertHandleEquals(expectedInvokeDynamicInsn.bsm, actualInvokeDynamicInsn.bsm);
				assertEquals(expectedInvokeDynamicInsn.bsmArgs.length, actualInvokeDynamicInsn.bsmArgs.length);
				for (int i = 0; i < expectedInvokeDynamicInsn.bsmArgs.length; i++) {
					assertConstantEquals(expectedInvokeDynamicInsn.bsmArgs[i], actualInvokeDynamicInsn.bsmArgs[i]);
				}
				break;
			case AbstractInsnNode.JUMP_INSN:
				JumpInsnNode expectedJumpInsn = (JumpInsnNode) expectedInsn;
				JumpInsnNode actualJumpInsn = (JumpInsnNode) actualInsn;
				assertEquals(expectedInsnIndexByLabel.get(expectedJumpInsn.label), actualInsnIndexByLabel.get(actualJumpInsn.label));
				break;
			case AbstractInsnNode.LDC_INSN:
				assertConstantEquals(((LdcInsnNode) expectedInsn).cst, ((LdcInsnNode) actualInsn).cst);
				break;
			case AbstractInsnNode.IINC_INSN:
				IincInsnNode expectedIincInsn = (IincInsnNode) expectedInsn;
				IincInsnNode actualIincInsn = (IincInsnNode) actualInsn;
				assertEquals(expectedIincInsn.var, actualIincInsn.var);
				assertEquals(expectedIincInsn.incr, actualIincInsn.incr);
				break;
			case AbstractInsnNode.TABLESWITCH_INSN:
				TableSwitchInsnNode expectedTableSwitchInsn = (TableSwitchInsnNode) expectedInsn;
				TableSwitchInsnNode actualTableSwitchInsn = (TableSwitchInsnNode) actualInsn;
				assertEquals(expectedTableSwitchInsn.min, actualTableSwitchInsn.min);
				assertEquals(expectedTableSwitchInsn.max, actualTableSwitchInsn.max);
				assertEquals(expectedInsnIndexByLabel.get(expectedTableSwitchInsn.dflt), actualInsnIndexByLabel.get(actualTableSwitchInsn.dflt));
				assertEquals(expectedTableSwitchInsn.labels.size(), actualTableSwitchInsn.labels.size());
				for (int i = 0; i < expectedTableSwitchInsn.labels.size(); i++) {
					assertEquals(expectedInsnIndexByLabel.get(expectedTableSwitchInsn.labels.get(i)), actualInsnIndexByLabel.get(actualTableSwitchInsn.labels.get(i)));
				}
				break;
			case AbstractInsnNode.LOOKUPSWITCH_INSN:
				LookupSwitchInsnNode expectedLookupSwitchInsn = (LookupSwitchInsnNode) expectedInsn;
				LookupSwitchInsnNode actualLookupSwitchInsn = (LookupSwitchInsnNode) actualInsn;
				assertEquals(expectedInsnIndexByLabel.get(expectedLookupSwitchInsn.dflt), actualInsnIndexByLabel.get(actualLookupSwitchInsn.dflt));
				assertEquals(expectedLookupSwitchInsn.keys, actualLookupSwitchInsn.keys);
				for (int i = 0; i < expectedLookupSwitchInsn.labels.size(); i++) {
					assertEquals(expectedInsnIndexByLabel.get(expectedLookupSwitchInsn.labels.get(i)), actualInsnIndexByLabel.get(actualLookupSwitchInsn.labels.get(i)));
				}
				break;
			case AbstractInsnNode.MULTIANEWARRAY_INSN:
				MultiANewArrayInsnNode expectedMultiANewArrayInsn = (MultiANewArrayInsnNode) expectedInsn;
				MultiANewArrayInsnNode actualMultiANewArrayInsn = (MultiANewArrayInsnNode) actualInsn;
				assertEquals(expectedMultiANewArrayInsn.desc, actualMultiANewArrayInsn.desc);
				assertEquals(expectedMultiANewArrayInsn.dims, actualMultiANewArrayInsn.dims);
				break;
			default:
				throw new AssertionError("Unknown insn type: " + expectedInsn.getClass().getName());
		}
	}

	private static void assertHandleEquals(Handle expectedHandle, Handle actualHandle) {
		assertEquals(expectedHandle.getTag(), actualHandle.getTag());
		assertEquals(expectedHandle.getOwner(), actualHandle.getOwner());
		assertEquals(expectedHandle.getName(), actualHandle.getName());
		assertEquals(expectedHandle.getDesc(), actualHandle.getDesc());
		assertEquals(expectedHandle.isInterface(), actualHandle.isInterface());
	}

	private static void assertCondyEquals(ConstantDynamic expectedCondy, ConstantDynamic actualCondy) {
		assertEquals(expectedCondy.getName(), actualCondy.getName());
		assertEquals(expectedCondy.getDescriptor(), actualCondy.getDescriptor());
		assertHandleEquals(expectedCondy.getBootstrapMethod(), actualCondy.getBootstrapMethod());
		assertEquals(expectedCondy.getBootstrapMethodArgumentCount(), actualCondy.getBootstrapMethodArgumentCount());
		for (int i = 0; i < expectedCondy.getBootstrapMethodArgumentCount(); i++) {
			assertConstantEquals(expectedCondy.getBootstrapMethodArgument(i), actualCondy.getBootstrapMethodArgument(i));
		}
	}

	private static void assertConstantEquals(Object expectedBsmArg, Object actualBsmArg) {
		if (expectedBsmArg instanceof Handle) {
			Handle expectedHandle = (Handle) expectedBsmArg;
			Handle actualHandle = assertInstanceOf(Handle.class, actualBsmArg);
			assertHandleEquals(expectedHandle, actualHandle);
		} else if (expectedBsmArg instanceof ConstantDynamic) {
			ConstantDynamic expectedCondy = (ConstantDynamic) expectedBsmArg;
			ConstantDynamic actualCondy = assertInstanceOf(ConstantDynamic.class, actualBsmArg);
			assertCondyEquals(expectedCondy, actualCondy);
		} else {
			assertEquals(expectedBsmArg, actualBsmArg);
		}
	}
}

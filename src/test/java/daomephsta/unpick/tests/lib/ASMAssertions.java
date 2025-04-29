package daomephsta.unpick.tests.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.util.Printer;

import daomephsta.unpick.impl.AbstractInsnNodes;
import daomephsta.unpick.impl.Utils;

public class ASMAssertions implements Opcodes {
	public static void assertInvokesMethod(AbstractInsnNode insn, Class<?> owner, String name, String descriptor) {
		assertInvokesMethod(insn, getInternalName(owner), name, descriptor);
	}

	public static void assertInvokesMethod(AbstractInsnNode insn, String owner, String name, String descriptor) {
		assertTrue(insn.getOpcode() >= INVOKEVIRTUAL && insn.getOpcode() <= INVOKEINTERFACE,
				"Instruction " + Utils.visitableToString(insn::accept).trim() + " does not invoke a method");
		MethodInsnNode invocationInsn = (MethodInsnNode) insn;
		assertEquals(owner, invocationInsn.owner);
		assertEquals(name, invocationInsn.name);
		assertEquals(descriptor, invocationInsn.desc);
	}

	public static void assertReadsField(AbstractInsnNode insn, Class<?> owner, String name, String descriptor) {
		assertReadsField(insn, getInternalName(owner), name, descriptor);
	}

	public static void assertReadsField(AbstractInsnNode insn, String owner, String name, String descriptor) {
		assertTrue(insn.getOpcode() == GETFIELD | insn.getOpcode() == GETSTATIC,
				"Instruction " + Utils.visitableToString(insn::accept).trim() + " does not read a field");
		FieldInsnNode fieldInsn = (FieldInsnNode) insn;
		assertEquals(owner, fieldInsn.owner);
		assertEquals(name, fieldInsn.name);
		assertEquals(descriptor, fieldInsn.desc);
	}

	public static void assertOpcode(AbstractInsnNode node, int expectedOpcode) {
		assertEquals(expectedOpcode, node.getOpcode(),
				String.format("expected: <%s> but was: <%s>", Printer.OPCODES[expectedOpcode], Utils.visitableToString(node::accept).trim()));
	}

	public static void assertIsLiteral(AbstractInsnNode node, Object expected) {
		assertTrue(AbstractInsnNodes.isLiteral(node, expected),
				String.format("expected: literal of <%s> but was: <%s>", expected, AbstractInsnNodes.getLiteralValue(node)));
	}

	private static String getInternalName(Class<?> clazz) {
		return clazz.getName().replace('.', '/');
	}
}

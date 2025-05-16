package daomephsta.unpick.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

public class TestNonStaticConstants {
	@Test
	public void testNonStaticConstants() {
		TestUtils.runTest(
				"pkg/TestNonStaticConstants",
				data -> {
					data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.INT).constant(new FieldExpression("pkg.TestNonStaticConstants", "foo", null, false)).build());
					data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.STRING)
							.constant(new FieldExpression("pkg.TestNonStaticConstants", "bar", null, false))
							.constant(new FieldExpression("pkg.TestNonStaticConstants", "nullConstant", null, false))
							.build());
					data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.CLASS).constant(new FieldExpression("pkg.TestNonStaticConstants", "baz", null, false)).build());
				},
				clazz -> {
					for (FieldNode field : clazz.fields) {
						field.value = null;
					}
					for (MethodNode method : clazz.methods) {
						List<AbstractInsnNode> instructionsToRemove = new ArrayList<>();
						for (AbstractInsnNode insn : method.instructions) {
							if (insn instanceof MethodInsnNode methodInsn && methodInsn.owner.equals("pkg/Constants") && methodInsn.name.startsWith("blackBox")) {
								instructionsToRemove.add(insn);
							}
						}
						for (AbstractInsnNode insn : instructionsToRemove) {
							method.instructions.remove(insn);
						}
					}
				});
	}

	@Test
	public void testNonStaticConstantInInnerClass() {
		TestUtils.runTest("pkg/TestNonStaticConstants$TestInner", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.INT).constant(new FieldExpression("pkg.TestNonStaticConstants", "foo", null, false)).build());
		});
	}

	@Test
	public void testNonStaticConstantInInnerInnerClass() {
		TestUtils.runTest("pkg/TestNonStaticConstants$TestInner$TestInnerInner", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.global(DataType.INT).constant(new FieldExpression("pkg.TestNonStaticConstants", "foo", null, false)).build());
		});
	}
}

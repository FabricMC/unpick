package daomephsta.unpick.tests;

import org.junit.jupiter.api.Test;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.tests.lib.TestUtils;

public class TestLambda {
	@Test
	public void testGroupOutsideToInsideCapture() {
		TestUtils.runTest("pkg/TestLambdaOutsideToInsideCapture", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "test")
					.constant(new FieldExpression("pkg.Constants", "INT_CONST_1", null, true))
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.TestLambdaOutsideToInsideCapture", "supplyInt", "()I")
					.returnGroup("test")
					.build());
		});
	}

	@Test
	public void testGroupInsideToOutsideCapture() {
		TestUtils.runTest("pkg/TestLambdaInsideToOutsideCapture", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "test")
					.constant(new FieldExpression("pkg.Constants", "INT_CONST_1", null, true))
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.Constants", "consumeInt", "(I)V")
					.paramGroup(0, "test")
					.build());
		});
	}

	@Test
	public void testGroupedFunctionalInterfaceParameter() {
		TestUtils.runTest("pkg/TestLambdaGroupedFunctionalInterfaceParameter", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "test")
					.constant(new FieldExpression("pkg.Constants", "INT_CONST_1", null, true))
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.TestLambdaGroupedFunctionalInterfaceParameter$I", "run", "(I)V")
					.paramGroup(0, "test")
					.build());
		});
	}

	@Test
	public void testGroupedFunctionalInterfaceReturn() {
		TestUtils.runTest("pkg/TestLambdaGroupedFunctionalInterfaceReturn", data -> {
			data.visitGroupDefinition(GroupDefinition.Builder.named(DataType.INT, "test")
					.constant(new FieldExpression("pkg.Constants", "INT_CONST_1", null, true))
					.build());
			data.visitTargetMethod(TargetMethod.Builder.builder("pkg.TestLambdaGroupedFunctionalInterfaceReturn$I", "run", "()I")
					.returnGroup("test")
					.build());
		});
	}
}

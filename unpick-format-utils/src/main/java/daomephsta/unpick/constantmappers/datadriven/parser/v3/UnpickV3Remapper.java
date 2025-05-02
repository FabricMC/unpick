package daomephsta.unpick.constantmappers.datadriven.parser.v3;

import java.util.List;
import java.util.stream.Stream;

import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionTransformer;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;

/**
 * Remaps all class, field, and method names in a .unpick v3 file. Visitor methods will be called on the downstream
 * visitor with the remapped names.
 */
public abstract class UnpickV3Remapper extends UnpickV3Visitor {
	private final UnpickV3Visitor downstream;

	/**
	 * Warning: class names use "." format, not "/" format. {@code classesInPackage} should contain all the classes in
	 * each package, including unmapped ones. The classes in this map are unqualified by the package name (because the
	 * package name is already in the key of the map entry).
	 */
	public UnpickV3Remapper(UnpickV3Visitor downstream) {
		this.downstream = downstream;
	}

	@Override
	public void visitGroupDefinition(GroupDefinition groupDefinition) {
		List<GroupScope> scopes = groupDefinition.scopes().stream()
				.flatMap(scope -> {
					if (scope instanceof GroupScope.Package packageScope) {
						return getClassesInPackage(packageScope.packageName()).stream()
								.map((className) -> new GroupScope.Class(mapClassName(className)));
					} else if (scope instanceof GroupScope.Class classScope) {
						return Stream.<GroupScope>of(new GroupScope.Class(mapClassName(classScope.className())));
					} else if (scope instanceof GroupScope.Method methodScope) {
						String className = mapClassName(methodScope.className());
						String methodName = mapMethodName(methodScope.className(), methodScope.methodName(), methodScope.methodDesc());
						String methodDesc = mapDescriptor(methodScope.methodDesc());
						return Stream.<GroupScope>of(new GroupScope.Method(className, methodName, methodDesc));
					} else {
						throw new AssertionError("Unknown group scope type: " + scope.getClass().getName());
					}
				})
				.toList();

		List<Expression> constants = groupDefinition.constants().stream()
				.map(constant -> constant.transform(new ExpressionRemapper()))
				.toList();

		downstream.visitGroupDefinition(GroupDefinition.Builder.from(groupDefinition).setScopes(scopes).setConstants(constants).build());
	}

	@Override
	public void visitTargetField(TargetField targetField) {
		String className = mapClassName(targetField.className());
		String fieldName = mapFieldName(targetField.className(), targetField.fieldName(), targetField.fieldDesc());
		String fieldDesc = mapDescriptor(targetField.fieldDesc());
		downstream.visitTargetField(new TargetField(className, fieldName, fieldDesc, targetField.groupName()));
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod) {
		String className = mapClassName(targetMethod.className());
		String methodName = mapMethodName(targetMethod.className(), targetMethod.methodName(), targetMethod.methodDesc());
		String methodDesc = mapDescriptor(targetMethod.methodDesc());
		downstream.visitTargetMethod(new TargetMethod(className, methodName, methodDesc, targetMethod.paramGroups(), targetMethod.returnGroup()));
	}

	protected abstract String mapClassName(String className);

	protected abstract String mapFieldName(String className, String fieldName, String fieldDesc);

	protected abstract String mapMethodName(String className, String methodName, String methodDesc);

	protected abstract List<String> getClassesInPackage(String pkg);

	protected abstract String getFieldDesc(String className, String fieldName);

	private String mapDescriptor(String descriptor) {
		StringBuilder mappedDescriptor = new StringBuilder();

		int semicolonIndex = 0;
		int lIndex;
		while ((lIndex = descriptor.indexOf('L', semicolonIndex)) != -1) {
			mappedDescriptor.append(descriptor, semicolonIndex, lIndex + 1);
			semicolonIndex = descriptor.indexOf(';', lIndex);
			if (semicolonIndex == -1) {
				throw new AssertionError("Invalid descriptor: " + descriptor);
			}
			String className = descriptor.substring(lIndex + 1, semicolonIndex).replace('/', '.');
			mappedDescriptor.append(mapClassName(className).replace('.', '/'));
		}

		return mappedDescriptor.append(descriptor, semicolonIndex, descriptor.length()).toString();
	}

	private class ExpressionRemapper extends ExpressionTransformer {
		@Override
		public Expression transformFieldExpression(FieldExpression fieldExpression) {
			String className = mapClassName(fieldExpression.className);

			if (fieldExpression.fieldName == null) {
				return new FieldExpression(className, null, fieldExpression.fieldType, fieldExpression.isStatic);
			}

			String fieldDesc;
			if (fieldExpression.fieldType == null) {
				fieldDesc = getFieldDesc(fieldExpression.className, fieldExpression.fieldName);
			} else {
				fieldDesc = switch (fieldExpression.fieldType) {
					case BYTE -> "B";
					case SHORT -> "S";
					case INT -> "I";
					case LONG -> "J";
					case FLOAT -> "F";
					case DOUBLE -> "D";
					case CHAR -> "C";
					case STRING -> "Ljava/lang/String;";
					case CLASS -> "Ljava/lang/Class;";
				};
			}

			String fieldName = mapFieldName(fieldExpression.className, fieldExpression.fieldName, fieldDesc);
			return new FieldExpression(className, fieldName, fieldExpression.fieldType, fieldExpression.isStatic);
		}
	}
}

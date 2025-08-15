package daomephsta.unpick.constantmappers.datadriven.parser.v3;

import java.util.List;
import java.util.stream.Stream;

import daomephsta.unpick.constantmappers.datadriven.tree.ForwardingUnpickV3Visitor;
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
 * visitor with the remapped names. <strong>All class names use "." format, not "/" format</strong>.
 */
public abstract class UnpickV3Remapper extends ForwardingUnpickV3Visitor {
	public UnpickV3Remapper(UnpickV3Visitor downstream) {
		super(downstream);
	}

	@Override
	public void visitGroupDefinition(GroupDefinition groupDefinition) {
		List<GroupScope> scopes = groupDefinition.scopes().stream()
				.flatMap(scope -> {
					switch (scope) {
						case GroupScope.Package(String packageName) -> {
							return getClassesInPackage(packageName).stream()
									.map((className) -> new GroupScope.Class(mapClassName(className)));
						}
						case GroupScope.Class(String className) -> {
							return Stream.<GroupScope>of(new GroupScope.Class(mapClassName(className)));
						}
						case GroupScope.Method(String className, String methodName, String methodDesc) -> {
							String newClassName = mapClassName(className);
							String newMethodName = mapMethodName(className, methodName, methodDesc);
							String newMethodDesc = mapDescriptor(methodDesc);
							return Stream.<GroupScope>of(new GroupScope.Method(newClassName, newMethodName, newMethodDesc));
						}
					}
				})
				.toList();

		List<Expression> constants = groupDefinition.constants().stream()
				.map(constant -> constant.transform(new ExpressionRemapper()))
				.toList();

		super.visitGroupDefinition(GroupDefinition.Builder.from(groupDefinition).setScopes(scopes).setConstants(constants).build());
	}

	@Override
	public void visitTargetField(TargetField targetField) {
		String className = mapClassName(targetField.className());
		String fieldName = mapFieldName(targetField.className(), targetField.fieldName(), targetField.fieldDesc());
		String fieldDesc = mapDescriptor(targetField.fieldDesc());
		super.visitTargetField(new TargetField(className, fieldName, fieldDesc, targetField.groupName()));
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod) {
		String className = mapClassName(targetMethod.className());
		String methodName = mapMethodName(targetMethod.className(), targetMethod.methodName(), targetMethod.methodDesc());
		String methodDesc = mapDescriptor(targetMethod.methodDesc());
		super.visitTargetMethod(new TargetMethod(className, methodName, methodDesc, targetMethod.paramGroups(), targetMethod.returnGroup()));
	}

	/**
	 * Maps a class name. The input class name uses "." format not "/" format, and so should the output class name.
	 */
	protected abstract String mapClassName(String className);

	/**
	 * Maps a field name. The input class name uses "." format not "/" format. The descriptor is as normal (using "/" for class names).
	 */
	protected abstract String mapFieldName(String className, String fieldName, String fieldDesc);

	/**
	 * Maps a method name. The input class name uses "." format not "/" format. The descriptor is as normal (using "/" for class names).
	 */
	protected abstract String mapMethodName(String className, String methodName, String methodDesc);

	/**
	 * Returns a list of all unmapped classes in the given unmapped package. The package name uses "." format not "/" format,
	 * and so should the output class names. The output class names should be fully qualified. Classes in subpackages are
	 * not considered as inside the parent package for the purposes of this method. That is, if the class {@code foo.bar.Baz}
	 * exists, then {@code getClassesInPackage("foo.bar")} should return that class, but {@code getClassesInPackage("foo")}
	 * should not.
	 */
	protected abstract List<String> getClassesInPackage(String pkg);

	/**
	 * Gets the descriptor of a field given only its name and owner. The input class name uses "." format not "/" format.
	 * The returned descriptor is as normal (using "/" for class names).
	 */
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

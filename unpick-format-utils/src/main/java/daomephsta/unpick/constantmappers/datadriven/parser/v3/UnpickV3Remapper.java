package daomephsta.unpick.constantmappers.datadriven.parser.v3;

import daomephsta.unpick.constantmappers.datadriven.parser.MemberKey;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionTransformer;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Remaps all class, field, and method names in a .unpick v3 file. Visitor methods will be called on the downstream
 * visitor with the remapped names.
 */
public class UnpickV3Remapper extends UnpickV3Visitor
{
	private final UnpickV3Visitor downstream;
	private final Map<String, List<String>> classesInPackage;
	private final Map<String, String> classMappings;
	private final Map<MemberKey, String> fieldMappings;
	private final Map<MemberKey, String> methodMappings;

	/**
	 * <strong>Warning</strong>: class names use "." format, not "/" format, including in the field and method mapping
	 * keys.
	 *
	 * <p>{@code classesInPackage} should contain all the classes in each package, including unmapped ones. The classes
	 * in this map are unqualified by the package name (because the package name is already in the key of the map entry).
	 */
	public UnpickV3Remapper(
		UnpickV3Visitor downstream,
		Map<String, List<String>> classesInPackage,
		Map<String, String> classMappings,
		Map<MemberKey, String> fieldMappings,
		Map<MemberKey, String> methodMappings
	)
	{
		this.downstream = downstream;
		this.classesInPackage = classesInPackage;
		this.classMappings = classMappings;
		this.fieldMappings = fieldMappings;
		this.methodMappings = methodMappings;
	}

	@Override
	public void visitGroupDefinition(GroupDefinition groupDefinition)
	{
		GroupScope oldScope = groupDefinition.scope;
		List<GroupScope> scopes;
		if (oldScope instanceof GroupScope.Global)
		{
			scopes = Collections.singletonList(oldScope);
		}
		else if (oldScope instanceof GroupScope.Package)
		{
			String pkg = ((GroupScope.Package) oldScope).packageName;
			scopes = classesInPackage.getOrDefault(pkg, Collections.emptyList()).stream()
				.map(cls -> new GroupScope.Class(mapClassName(pkg + "." + cls)))
				.collect(Collectors.toList());
		}
		else if (oldScope instanceof GroupScope.Class)
		{
			scopes = Collections.singletonList(new GroupScope.Class(mapClassName(((GroupScope.Class) oldScope).className)));
		}
		else if (oldScope instanceof GroupScope.Method)
		{
			GroupScope.Method methodScope = (GroupScope.Method) oldScope;
			String className = mapClassName(methodScope.className);
			String methodName = mapMethodName(methodScope.className, methodScope.methodName, methodScope.methodDesc);
			String methodDesc = mapDescriptor(methodScope.methodDesc);
			scopes = Collections.singletonList(new GroupScope.Method(className, methodName, methodDesc));
		}
		else
		{
			throw new AssertionError("Unknown group scope type: " + oldScope.getClass().getName());
		}

		List<GroupConstant> constants = groupDefinition.constants.stream()
			.map(constant -> new GroupConstant(mapConstantKey(constant.key), constant.value.transform(new ExpressionRemapper(groupDefinition.dataType))))
			.collect(Collectors.toList());

		for (GroupScope scope : scopes)
		{
			downstream.visitGroupDefinition(GroupDefinition.Builder.from(groupDefinition).scoped(scope).constants(constants).build());
		}
	}

	@Override
	public void visitTargetField(TargetField targetField)
	{
		String className = mapClassName(targetField.className);
		String fieldName = mapFieldName(targetField.className, targetField.fieldName, targetField.fieldDesc);
		String fieldDesc = mapDescriptor(targetField.fieldDesc);
		downstream.visitTargetField(new TargetField(className, fieldName, fieldDesc, targetField.groupName));
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod)
	{
		String className = mapClassName(targetMethod.className);
		String methodName = mapMethodName(targetMethod.className, targetMethod.methodName, targetMethod.methodDesc);
		String methodDesc = mapDescriptor(targetMethod.methodDesc);
		downstream.visitTargetMethod(new TargetMethod(className, methodName, methodDesc, targetMethod.paramGroups, targetMethod.returnGroup));
	}

	private String mapClassName(String className)
	{
		return classMappings.getOrDefault(className, className);
	}

	private String mapFieldName(String className, String fieldName, String fieldDesc)
	{
		return fieldMappings.getOrDefault(new MemberKey(className, fieldName, fieldDesc), fieldName);
	}

	private String mapMethodName(String className, String methodName, String methodDesc)
	{
		return methodMappings.getOrDefault(new MemberKey(className, methodName, methodDesc), methodName);
	}

	private Literal.ConstantKey mapConstantKey(Literal.ConstantKey constantKey)
	{
		if (constantKey instanceof Literal.Class)
		{
			return new Literal.Class(mapDescriptor(((Literal.Class) constantKey).descriptor));
		}
		else
		{
			return constantKey;
		}
	}

	private String mapDescriptor(String descriptor)
	{
		StringBuilder mappedDescriptor = new StringBuilder();

		int semicolonIndex = 0;
		int lIndex;
		while ((lIndex = descriptor.indexOf('L', semicolonIndex)) != -1)
		{
			mappedDescriptor.append(descriptor, semicolonIndex, lIndex + 1);
			semicolonIndex = descriptor.indexOf(';', lIndex);
			if (semicolonIndex == -1)
			{
				throw new AssertionError("Invalid descriptor: " + descriptor);
			}
			String className = descriptor.substring(lIndex + 1, semicolonIndex).replace('/', '.');
			mappedDescriptor.append(mapClassName(className).replace('.', '/'));
		}

		return mappedDescriptor.append(descriptor, semicolonIndex, descriptor.length()).toString();
	}

	private class ExpressionRemapper extends ExpressionTransformer
	{
		private final DataType groupDataType;

		ExpressionRemapper(DataType groupDataType)
		{
			this.groupDataType = groupDataType;
		}

		@Override
		public Expression transformFieldExpression(FieldExpression fieldExpression)
		{
			String fieldDesc;
			switch (fieldExpression.fieldType == null ? groupDataType : fieldExpression.fieldType)
			{
				case BYTE:
					fieldDesc = "B";
					break;
				case SHORT:
					fieldDesc = "S";
					break;
				case INT:
					fieldDesc = "I";
					break;
				case LONG:
					fieldDesc = "J";
					break;
				case FLOAT:
					fieldDesc = "F";
					break;
				case DOUBLE:
					fieldDesc = "D";
					break;
				case CHAR:
					fieldDesc = "C";
					break;
				case STRING:
					fieldDesc = "Ljava/lang/String;";
					break;
				case CLASS:
					fieldDesc = "Ljava/lang/Class;";
					break;
				default:
					throw new AssertionError("Unknown data type: " + fieldExpression.fieldType);
			}

			String className = mapClassName(fieldExpression.className);
			String fieldName = mapFieldName(fieldExpression.className, fieldExpression.fieldName, fieldDesc);
			return new FieldExpression(className, fieldName, fieldExpression.fieldType, fieldExpression.isStatic);
		}
	}
}

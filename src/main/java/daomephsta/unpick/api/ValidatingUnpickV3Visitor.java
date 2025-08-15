package daomephsta.unpick.api;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IMemberChecker;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.ForwardingUnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.impl.DataTypeUtils;
import daomephsta.unpick.impl.constantmappers.datadriven.data.Data;

public abstract class ValidatingUnpickV3Visitor extends ForwardingUnpickV3Visitor {
	private final IMemberChecker memberChecker;
	private final Data data;

	private final Map<String, DataType> actualGroupTypes = new HashMap<>();
	private final Map<String, Set<DataType>> expectedGroupTypes = new HashMap<>();

	public ValidatingUnpickV3Visitor(IClassResolver classResolver) {
		this(classResolver, null);
	}

	public ValidatingUnpickV3Visitor(IClassResolver classResolver, @Nullable UnpickV3Visitor downstream) {
		super(downstream);
		this.memberChecker = classResolver.asMemberChecker();
		// null logger is ok because lenient is false, so exceptions are thrown instead
		this.data = new Data(null, false, classResolver.asConstantResolver(), classResolver.asInheritanceChecker());
	}

	public abstract boolean packageExists(String packageName);

	@Override
	public void visitGroupDefinition(GroupDefinition groupDefinition) {
		if (groupDefinition.name() != null) {
			actualGroupTypes.put(groupDefinition.name(), groupDefinition.dataType());
		}

		for (GroupScope scope : groupDefinition.scopes()) {
			switch (scope) {
				case GroupScope.Package packageScope -> {
					if (!packageExists(packageScope.packageName())) {
						throw new UnpickSyntaxException("Package " + packageScope.packageName() + " does not exist");
					}
				}
				case GroupScope.Class classScope -> {
					if (memberChecker.getFields(classScope.className().replace('.', '/')) == null) {
						throw new UnpickSyntaxException("Class " + classScope.className() + " does not exist");
					}
				}
				case GroupScope.Method methodScope -> {
					if (memberChecker.getMethod(methodScope.className().replace('.', '/'), methodScope.methodName(), methodScope.methodDesc()) == null) {
						throw new UnpickSyntaxException("Method " + methodScope.className() + "." + methodScope.methodName() + methodScope.methodDesc() + " does not exist");
					}
				}
			}
		}

		data.visitGroupDefinition(groupDefinition);
		super.visitGroupDefinition(groupDefinition);
	}

	@Override
	public void visitTargetField(TargetField targetField) {
		if (memberChecker.getField(targetField.className().replace('.', '/'), targetField.fieldName(), targetField.fieldDesc()) == null) {
			throw new UnpickSyntaxException("No such field: " + targetField.className() + "." + targetField.fieldName() + ":" + targetField.fieldDesc());
		}

		validateGroupType(targetField.groupName(), getDataTypeFromType(Type.getType(targetField.fieldDesc())));

		data.visitTargetField(targetField);
		super.visitTargetField(targetField);
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod) {
		if (memberChecker.getMethod(targetMethod.className().replace('.', '/'), targetMethod.methodName(), targetMethod.methodDesc()) == null) {
			throw new UnpickSyntaxException("No such method: " + targetMethod.className() + "." + targetMethod.methodName() + targetMethod.methodDesc());
		}

		if (targetMethod.returnGroup() != null) {
			validateGroupType(targetMethod.returnGroup(), getDataTypeFromType(Type.getReturnType(targetMethod.methodDesc())));
		}

		Type[] paramTypes = Type.getArgumentTypes(targetMethod.methodDesc());
		targetMethod.paramGroups().forEach((paramIndex, paramGroup) -> {
			if (paramIndex < 0 || paramIndex >= paramTypes.length) {
				throw new UnpickSyntaxException("Parameter index out of bounds: " + paramIndex);
			}

			validateGroupType(paramGroup, getDataTypeFromType(paramTypes[paramIndex]));
		});

		data.visitTargetMethod(targetMethod);
		super.visitTargetMethod(targetMethod);
	}

	public void finishValidation() {
		expectedGroupTypes.forEach((groupName, expectedTypes) -> {
			DataType actualType = actualGroupTypes.get(groupName);
			if (actualType == null) {
				throw new UnpickSyntaxException("Reference to undeclared group: " + groupName);
			}

			for (DataType expectedType : expectedTypes) {
				boolean compatible;
				if (expectedType == DataType.CHAR) {
					compatible = actualType == DataType.INT || actualType == DataType.LONG;
				} else if (DataTypeUtils.isPrimitive(expectedType)) {
					compatible = DataTypeUtils.isPrimitive(actualType);
				} else {
					compatible = expectedType == actualType;
				}

				if (!compatible) {
					throw new UnpickSyntaxException("Target of type " + DataTypeUtils.getTypeName(expectedType) + " declares group " + groupName + " of incompatible type " + DataTypeUtils.getTypeName(actualType));
				}
			}
		});
	}

	private DataType getDataTypeFromType(Type type) {
		DataType result = DataTypeUtils.asmTypeToDataType(type);
		if (result == null) {
			throw new UnpickSyntaxException("Not an unpickable data type: " + type.getClassName());
		}

		return result;
	}

	private void validateGroupType(String groupName, DataType expectedType) {
		expectedGroupTypes.computeIfAbsent(groupName, k -> EnumSet.noneOf(DataType.class)).add(expectedType);
	}
}

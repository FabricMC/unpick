package daomephsta.unpick.api;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IMemberChecker;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.ForwardingUnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetAnnotation;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.impl.DataTypeUtils;
import daomephsta.unpick.impl.constantmappers.datadriven.data.Data;

/**
 * An {@link UnpickV3Visitor} which fully validates or lints its input. Note that it only performs checks so far as data
 * that could be parsed from a valid unpick definition file. It will not check for example invalid type descriptor created
 * via code.
 *
 * <p>Once everything has been visited, call {@link #finishValidation()} to finish the validation and obtain a list of
 * errors.
 */
public abstract class ValidatingUnpickV3Visitor extends ForwardingUnpickV3Visitor {
	private final IClassResolver classResolver;
	private final IMemberChecker memberChecker;
	private final Data data;

	private final Map<String, DataType> actualGroupTypes = new HashMap<>();
	private final Map<String, Set<DataType>> expectedGroupTypes = new HashMap<>();

	private final List<UnpickSyntaxException> errors = new ArrayList<>();

	public ValidatingUnpickV3Visitor(IClassResolver classResolver) {
		this(classResolver, null);
	}

	public ValidatingUnpickV3Visitor(IClassResolver classResolver, @Nullable UnpickV3Visitor downstream) {
		super(downstream);
		this.classResolver = classResolver;
		this.memberChecker = classResolver.asMemberChecker();
		// null logger is ok because lenient is false, so exceptions are thrown instead
		this.data = new Data(null, false, classResolver.asConstantResolver(), classResolver.asInheritanceChecker());
	}

	public abstract boolean packageExists(String packageName);

	@Override
	public void visitGroupDefinition(GroupDefinition groupDefinition) {
		try {
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
		} catch (UnpickSyntaxException e) {
			errors.add(e);
		}

		super.visitGroupDefinition(groupDefinition);
	}

	@Override
	public void visitTargetField(TargetField targetField) {
		try {
			if (memberChecker.getField(targetField.className().replace('.', '/'), targetField.fieldName(), targetField.fieldDesc()) == null) {
				throw new UnpickSyntaxException("No such field: " + targetField.className() + "." + targetField.fieldName() + ":" + targetField.fieldDesc());
			}

			validateGroupType(targetField.groupName(), getDataTypeFromType(Type.getType(targetField.fieldDesc())));

			data.visitTargetField(targetField);
		} catch (UnpickSyntaxException e) {
			errors.add(e);
		}

		super.visitTargetField(targetField);
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod) {
		try {
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
		} catch (UnpickSyntaxException e) {
			errors.add(e);
		}

		super.visitTargetMethod(targetMethod);
	}

	@Override
	public void visitTargetAnnotation(TargetAnnotation targetAnnotation) {
		try {
			// check annotation exists
			ClassNode node = classResolver.resolveClass(targetAnnotation.annotationName().replace('.', '/'));

			if (node == null) {
				throw new UnpickSyntaxException("No such annotation: " + targetAnnotation.annotationName());
			}

			if ((node.access & Opcodes.ACC_ANNOTATION) == 0) {
				throw new UnpickSyntaxException("Not an annotation: " + targetAnnotation.annotationName());
			}

			data.visitTargetAnnotation(targetAnnotation);
		} catch (UnpickSyntaxException e) {
			errors.add(e);
		}

		super.visitTargetAnnotation(targetAnnotation);
	}

	public List<UnpickSyntaxException> finishValidation() {
		expectedGroupTypes.forEach((groupName, expectedTypes) -> {
			DataType actualType = actualGroupTypes.get(groupName);
			if (actualType == null) {
				errors.add(new UnpickSyntaxException("Reference to undeclared group: " + groupName));
				return;
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
					errors.add(new UnpickSyntaxException("Target of type " + DataTypeUtils.getTypeName(expectedType) + " declares group " + groupName + " of incompatible type " + DataTypeUtils.getTypeName(actualType)));
				}
			}
		});

		return errors;
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

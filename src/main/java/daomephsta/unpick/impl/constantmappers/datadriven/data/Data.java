package daomephsta.unpick.impl.constantmappers.datadriven.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.constantmappers.datadriven.parser.MemberKey;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionTransformer;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionVisitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.impl.DataTypeUtils;
import daomephsta.unpick.impl.constantmappers.datadriven.ExpressionEvaluator;

public final class Data extends UnpickV3Visitor {
	public final IConstantResolver constantResolver;
	public final IInheritanceChecker inheritanceChecker;
	public final Map<DataType, GroupInfo> defaultGroups = new EnumMap<>(DataType.class);
	public final Map<String, GroupInfo> groups = new HashMap<>();
	public final Map<MemberKey, TargetField> targetFields = new HashMap<>();
	public final Map<MemberKey, TargetMethod> targetMethods = new HashMap<>();

	public Data(IConstantResolver constantResolver, IInheritanceChecker inheritanceChecker) {
		this.constantResolver = constantResolver;
		this.inheritanceChecker = inheritanceChecker;
	}

	@Override
	public void visitGroupDefinition(GroupDefinition groupDefinition) {
		GroupInfo existingInfo = groupDefinition.name() == null
				? defaultGroups.computeIfAbsent(groupDefinition.dataType(), k -> new GroupInfo(groupDefinition.dataType(), groupDefinition.flags()))
				: groups.computeIfAbsent(groupDefinition.name(), k -> new GroupInfo(groupDefinition.dataType(), groupDefinition.flags()));
		String groupDisplayName = groupDefinition.name() == null ? "<default>" : groupDefinition.name();

		if (existingInfo.flags != groupDefinition.flags()) {
			throw new UnpickSyntaxException("Flags mismatch for group " + groupDisplayName);
		}
		if (existingInfo.dataType != groupDefinition.dataType()) {
			throw new UnpickSyntaxException("Data type mismatch for group " + groupDisplayName);
		}

		if (groupDefinition.scopes().isEmpty()) {
			addScopedGroupDefinition(groupDefinition, existingInfo.globalScope, groupDisplayName);
		} else {
			for (GroupScope scope : groupDefinition.scopes()) {
				ScopedGroupInfo existingScopedInfo = switch (scope) {
					case GroupScope.Package(String packageName) ->
							existingInfo.packageScopes.computeIfAbsent(packageName, k -> new ScopedGroupInfo());
					case GroupScope.Class(String className) ->
							existingInfo.classScopes.computeIfAbsent(className, k -> new ScopedGroupInfo());
					case GroupScope.Method(String className, String methodName, String methodDesc) ->
							existingInfo.methodScopes.computeIfAbsent(new MemberKey(className, methodName, methodDesc), k -> new ScopedGroupInfo());
				};

				addScopedGroupDefinition(groupDefinition, existingScopedInfo, groupDisplayName);
			}
		}
	}

	private void addScopedGroupDefinition(GroupDefinition groupDefinition, ScopedGroupInfo existingScopedInfo, String groupDisplayName) {
		if (groupDefinition.format() != null) {
			if (existingScopedInfo.format != groupDefinition.format() && existingScopedInfo.format != null) {
				throw new UnpickSyntaxException("Format mismatch for group " + groupDisplayName);
			}

			existingScopedInfo.format = groupDefinition.format();
		}

		for (Expression expression : groupDefinition.constants()) {
			for (Expression nonWildcardExpression : replaceWildcardIfNecessary(expression, groupDefinition.dataType(), groupDisplayName)) {
				ExpressionEvaluator evaluator = new ExpressionEvaluator(constantResolver, inheritanceChecker);
				nonWildcardExpression.accept(evaluator);
				Object value = DataTypeUtils.cast(evaluator.getResult(), groupDefinition.dataType());

				if (existingScopedInfo.constantReplacementMap.put(value, new ConstantReplacementInfo(groupDefinition.strict(), nonWildcardExpression)) != null) {
					throw new UnpickSyntaxException("Duplicate constant in group " + groupDisplayName + ": " + value);
				}
			}
		}
	}

	private List<Expression> replaceWildcardIfNecessary(Expression expression, DataType groupType, String groupDisplayName) {
		FieldExpression wildcardExpr = findWildcardFieldExpression(expression, groupDisplayName);
		if (wildcardExpr == null) {
			return List.of(expression);
		}

		Map<String, IConstantResolver.ResolvedConstant> constants = constantResolver.getAllConstantsInClass(wildcardExpr.className.replace('.', '/'));
		if (constants == null) {
			throw new UnpickSyntaxException("Could not resolve class " + wildcardExpr.className + " in group " + groupDisplayName);
		}

		List<Expression> replacements = new ArrayList<>();
		constants.forEach((fieldName, resolvedConstant) -> {
			if (resolvedConstant.isStatic() != wildcardExpr.isStatic) {
				return;
			}

			DataType resolvedConstantType = DataTypeUtils.asmTypeToDataType(resolvedConstant.type());
			if (wildcardExpr.fieldType != null) {
				if (resolvedConstantType != wildcardExpr.fieldType) {
					return;
				}
			} else {
				if (!DataTypeUtils.isAssignable(groupType, resolvedConstantType)) {
					return;
				}
			}

			Expression replacement = expression.transform(new ExpressionTransformer() {
				@Override
				public Expression transformFieldExpression(FieldExpression fieldExpression) {
					if (fieldExpression.fieldName == null) {
						return new FieldExpression(fieldExpression.className, fieldName, resolvedConstantType, fieldExpression.isStatic);
					} else {
						return super.transformFieldExpression(fieldExpression);
					}
				}
			});
			replacements.add(replacement);
		});

		if (replacements.isEmpty()) {
			throw new UnpickSyntaxException("Could not resolve wildcard field expression " + wildcardExpr.className + ".* in group " + groupDisplayName);
		}

		return replacements;
	}

	@Nullable
	private static FieldExpression findWildcardFieldExpression(Expression expression, String groupDisplayName) {
		FieldExpression[] result = {null};
		expression.accept(new ExpressionVisitor() {
			@Override
			public void visitFieldExpression(FieldExpression fieldExpression) {
				if (fieldExpression.fieldName == null) {
					if (result[0] != null) {
						throw new UnpickSyntaxException("Expression in group " + groupDisplayName + " has multiple wildcard field expressions");
					}
					result[0] = fieldExpression;
				}
			}
		});
		return result[0];
	}

	@Override
	public void visitTargetField(TargetField targetField) {
		if (targetFields.put(new MemberKey(targetField.className(), targetField.fieldName(), targetField.fieldDesc()), targetField) != null) {
			throw new UnpickSyntaxException("Duplicate target field: " + targetField.className() + "." + targetField.fieldName());
		}
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod) {
		MemberKey key = new MemberKey(targetMethod.className(), targetMethod.methodName(), targetMethod.methodDesc());
		TargetMethod existingTargetMethod = targetMethods.get(key);
		if (existingTargetMethod == null) {
			targetMethods.put(key, targetMethod);
			return;
		}

		Map<Integer, String> paramGroups = new HashMap<>(existingTargetMethod.paramGroups());
		targetMethod.paramGroups().forEach((paramIndex, groupName) -> {
			if (paramGroups.put(paramIndex, groupName) != null) {
				throw new UnpickSyntaxException("Duplicate param group: " + targetMethod.className() + "." + targetMethod.methodName() + targetMethod.methodDesc() + " " + paramIndex);
			}
		});

		String returnGroup;
		if (targetMethod.returnGroup() != null) {
			if (existingTargetMethod.returnGroup() != null) {
				throw new UnpickSyntaxException("Duplicate return group: " + targetMethod.className() + "." + targetMethod.methodName() + targetMethod.methodDesc());
			}
			returnGroup = targetMethod.returnGroup();
		} else {
			returnGroup = existingTargetMethod.returnGroup();
		}

		TargetMethod merged = new TargetMethod(
				targetMethod.className(),
				targetMethod.methodName(),
				targetMethod.methodDesc(),
				paramGroups,
				returnGroup
		);
		targetMethods.put(key, merged);
	}
}

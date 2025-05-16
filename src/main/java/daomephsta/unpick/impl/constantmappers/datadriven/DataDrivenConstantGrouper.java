package daomephsta.unpick.impl.constantmappers.datadriven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.api.constantgroupers.ConstantGroup;
import daomephsta.unpick.api.constantgroupers.IConstantGrouper;
import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.constantmappers.datadriven.parser.MemberKey;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Reader;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
import daomephsta.unpick.impl.AbstractInsnNodes;
import daomephsta.unpick.impl.DataTypeUtils;
import daomephsta.unpick.impl.constantmappers.datadriven.data.ConstantReplacementInfo;
import daomephsta.unpick.impl.constantmappers.datadriven.data.Data;
import daomephsta.unpick.impl.constantmappers.datadriven.data.GroupInfo;
import daomephsta.unpick.impl.constantmappers.datadriven.data.ScopedGroupInfo;
import daomephsta.unpick.impl.constantmappers.datadriven.parser.V1Parser;
import daomephsta.unpick.impl.constantmappers.datadriven.parser.v2.V2Parser;

/**
 * Maps inlined values to constants using mappings defined in a file.
 * @author Daomephsta
 */
public class DataDrivenConstantGrouper implements IConstantGrouper {
	private static final Logger LOGGER = Logger.getLogger("unpick");

	private final IInheritanceChecker inheritanceChecker;
	private final Data data;
	private final Map<MemberKey, TargetMethod> targetMethodCache = new ConcurrentHashMap<>();
	private final Set<MemberKey> noTargetMethodCache = ConcurrentHashMap.newKeySet();
	private final ConstantGroup defaultGroup = new ConstantGroup("<default>", this::replaceDefault);

	public DataDrivenConstantGrouper(IConstantResolver constantResolver, IInheritanceChecker inheritanceChecker, InputStream... mappingSources) {
		this.inheritanceChecker = inheritanceChecker;
		this.data = new Data(constantResolver, inheritanceChecker);
		for (InputStream mappingSource : mappingSources) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(mappingSource, StandardCharsets.UTF_8));
			try {
				reader.mark(11);
				String versionHeader = reader.readLine();
				reader.reset();

				switch (versionHeader) {
					case "v1" -> {
						if (constantResolver == null) {
							throw new UnpickSyntaxException(1, "Unpick V1 format is no longer supported");
						}
						V1Parser.parse(reader, constantResolver, data);
					}
					case "v2" -> {
						if (constantResolver == null) {
							throw new UnpickSyntaxException(1, "Unpick V2 format is no longer supported");
						}
						V2Parser.parse(reader, constantResolver, data);
					}
					case "unpick v3" -> new UnpickV3Reader(reader).accept(data);
					default ->
							throw new UnpickSyntaxException(1, "Unknown version or missing version header: " + versionHeader);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		LOGGER.info(() -> String.format("Loaded %d constant groups, %d target fields and %d target methods", data.defaultGroups.size() + data.groups.size(), data.targetFields.size(), data.targetMethods.size()));
	}

	@ApiStatus.Internal
	@VisibleForTesting
	public DataDrivenConstantGrouper(IConstantResolver constantResolver, IInheritanceChecker inheritanceChecker, Consumer<UnpickV3Visitor> dataProvider) {
		this.inheritanceChecker = inheritanceChecker;
		this.data = new Data(constantResolver, inheritanceChecker);
		dataProvider.accept(data);
	}

	@Override
	@Nullable
	public ConstantGroup getFieldGroup(String fieldOwner, String fieldName, String fieldDescriptor) {
		TargetField targetField = data.targetFields.get(new MemberKey(fieldOwner.replace('/', '.'), fieldName, fieldDescriptor));
		return targetField == null ? null : getGroupByName(targetField.groupName());
	}

	@Override
	@Nullable
	public ConstantGroup getMethodReturnGroup(String methodOwner, String methodName, String methodDescriptor) {
		TargetMethod targetMethod = findTargetMethod(methodOwner, methodName, methodDescriptor);
		return targetMethod == null || targetMethod.returnGroup() == null ? null : getGroupByName(targetMethod.returnGroup());
	}

	@Override
	@Nullable
	public ConstantGroup getMethodParameterGroup(String methodOwner, String methodName, String methodDescriptor, int parameterIndex) {
		TargetMethod targetMethod = findTargetMethod(methodOwner, methodName, methodDescriptor);
		if (targetMethod == null) {
			return null;
		}
		String groupName = targetMethod.paramGroups().get(parameterIndex);
		return groupName == null ? null : getGroupByName(groupName);
	}

	private TargetMethod findTargetMethod(String methodOwner, String methodName, String methodDescriptor) {
		MemberKey memberKey = new MemberKey(methodOwner.replace('/', '.'), methodName, methodDescriptor);
		if (noTargetMethodCache.contains(memberKey)) {
			return null;
		}

		TargetMethod targetMethod = targetMethodCache.get(memberKey);
		if (targetMethod != null) {
			return targetMethod;
		}

		targetMethod = data.targetMethods.get(memberKey);
		if (targetMethod == null) {
			IInheritanceChecker.ClassInfo classInfo = inheritanceChecker.getClassInfo(methodOwner);
			if (classInfo != null) {
				if (classInfo.superClass() != null) {
					targetMethod = findTargetMethod(classInfo.superClass(), methodName, methodDescriptor);
				}

				if (targetMethod == null) {
					for (String itf : classInfo.interfaces()) {
						targetMethod = findTargetMethod(itf, methodName, methodDescriptor);
						if (targetMethod != null) {
							break;
						}
					}
				}
			}
		}

		if (targetMethod == null) {
			noTargetMethodCache.add(memberKey);
		} else {
			targetMethodCache.put(memberKey, targetMethod);
		}

		return targetMethod;
	}

	@Override
	public ConstantGroup getDefaultGroup() {
		return defaultGroup;
	}

	private ConstantGroup getGroupByName(String name) {
		GroupInfo groupInfo = data.groups.get(name);
		if (groupInfo == null) {
			return null;
		}

		return new ConstantGroup(name, context -> replaceWithGroup(context, groupInfo));
	}

	private void replaceDefault(IReplacementGenerator.IContext context) {
		AbstractInsnNode target = context.getTarget();
		if (!AbstractInsnNodes.hasLiteralValue(target)) {
			return;
		}

		Object literal = AbstractInsnNodes.getLiteralValue(target);
		DataType literalType = DataTypeUtils.getDataType(literal);
		List<DataType> compatibleTypes = switch (literalType) {
			case LONG -> List.of(DataType.LONG, DataType.INT);
			case FLOAT -> List.of(DataType.FLOAT, DataType.LONG, DataType.INT);
			case DOUBLE -> List.of(DataType.DOUBLE, DataType.FLOAT, DataType.LONG, DataType.INT);
			case null -> {
				// use dataflow to figure out whether this is a null string constant, class constant or neither
				AbstractInsnNode nextInsn = AbstractInsnNodes.nextInstruction(target);
				if (nextInsn == null) {
					yield null;
				}
				Frame<IReplacementGenerator.IDataflowValue> frame = context.getDataflowFrame(nextInsn);
				if (frame == null) {
					yield null;
				}
				Set<DataType> typeInterpretations = frame.getStack(frame.getStackSize() - 1).getTypeInterpretations();
				if (typeInterpretations.contains(DataType.STRING)) {
					literalType = DataType.STRING;
					yield List.of(DataType.STRING);
				} else if (typeInterpretations.contains(DataType.CLASS)) {
					literalType = DataType.CLASS;
					yield List.of(DataType.CLASS);
				} else {
					yield null;
				}
			}
			default -> List.of(literalType);
		};
		if (compatibleTypes == null) {
			return;
		}

		for (DataType compatibleType : compatibleTypes) {
			Object castedLiteral = DataTypeUtils.tryCastExact(literal, DataTypeUtils.widenNarrowTypes(compatibleType));
			if (castedLiteral == null && literal != null) {
				continue;
			}

			GroupInfo defaultGroup = data.defaultGroups.get(compatibleType);
			if (defaultGroup == null) {
				continue;
			}

			for (ScopedGroupInfo scope : findMatchingScopes(context, defaultGroup)) {
				ConstantReplacementInfo replacementInfo = scope.constantReplacementMap.get(castedLiteral);
				if (replacementInfo != null && (!replacementInfo.strict() || compatibleType == literalType)) {
					DataType narrowedLiteralType = getNarrowedLiteralType(context, target, literalType, literal);
					ExpressionGenerator.replaceWithExpression(context, defaultGroup, replacementInfo.replacementExpression(), narrowedLiteralType);
					return;
				}
			}
		}
	}

	private void replaceWithGroup(IReplacementGenerator.IContext context, GroupInfo groupInfo) {
		AbstractInsnNode target = context.getTarget();
		if (!AbstractInsnNodes.hasLiteralValue(target)) {
			return;
		}

		Object literal = AbstractInsnNodes.getLiteralValue(target);
		DataType literalType = DataTypeUtils.getDataType(literal);
		if (literal == null) {
			if (DataTypeUtils.isPrimitive(groupInfo.dataType)) {
				return;
			}
		} else {
			if (!DataTypeUtils.isAssignable(literalType, groupInfo.dataType)) {
				return;
			}
		}

		Object castedLiteral = DataTypeUtils.tryCastExact(literal, groupInfo.dataType);
		if (castedLiteral == null && literal != null) {
			return;
		}

		Long longLiteral = (Long) DataTypeUtils.tryCastExact(literal, DataType.LONG);

		if (groupInfo.flags && DataTypeUtils.isAssignable(DataType.LONG, literalType) && longLiteral != null && !longLiteral.equals(0L) && !longLiteral.equals(-1L)) {
			DataType narrowedLiteralType = getNarrowedLiteralType(context, target, literalType, literal);
			Expression flagsExpression = ExpressionGenerator.generateFlagsExpression(context, groupInfo, longLiteral, literalType, narrowedLiteralType);
			if (flagsExpression != null) {
				ExpressionGenerator.replaceWithExpression(context, groupInfo, flagsExpression, narrowedLiteralType);
			}
		} else {
			for (ScopedGroupInfo scope : findMatchingScopes(context, groupInfo)) {
				ConstantReplacementInfo replacementInfo = scope.constantReplacementMap.get(castedLiteral);
				if (replacementInfo != null && (!replacementInfo.strict() || literalType == groupInfo.dataType)) {
					DataType narrowedLiteralType = getNarrowedLiteralType(context, target, literalType, literal);
					ExpressionGenerator.replaceWithExpression(context, groupInfo, replacementInfo.replacementExpression(), narrowedLiteralType);
					return;
				}
			}
		}
	}

	private static DataType getNarrowedLiteralType(IReplacementGenerator.IContext context, AbstractInsnNode target, DataType literalType, Object literal) {
		if (literalType != DataType.INT) {
			return literalType;
		}

		int value = (Integer) literal;

		AbstractInsnNode nextInsn = AbstractInsnNodes.nextInstruction(target);
		if (nextInsn == null) {
			return literalType;
		}
		Frame<IReplacementGenerator.IDataflowValue> dataflowFrame = context.getDataflowFrame(nextInsn);
		if (dataflowFrame == null) {
			return literalType;
		}
		Set<DataType> narrowTypeInterpretations = dataflowFrame.getStack(dataflowFrame.getStackSize() - 1).getTypeInterpretations();

		if (narrowTypeInterpretations.contains(DataType.BYTE) && value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
			return DataType.BYTE;
		} else if (narrowTypeInterpretations.contains(DataType.SHORT) && value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
			return DataType.SHORT;
		} else if (narrowTypeInterpretations.contains(DataType.CHAR) && value >= Character.MIN_VALUE && value <= Character.MAX_VALUE) {
			return DataType.CHAR;
		} else {
			return literalType;
		}
	}

	static List<ScopedGroupInfo> findMatchingScopes(IReplacementGenerator.IContext context, GroupInfo groupInfo) {
		List<ScopedGroupInfo> scopes = new ArrayList<>(1);

		String className = context.getContainingClass().name.replace('/', '.');
		MethodNode method = context.getContainingMethod();

		ScopedGroupInfo methodScope = groupInfo.methodScopes.get(new MemberKey(className, method.name, method.desc));
		if (methodScope != null) {
			scopes.add(methodScope);
		}

		ScopedGroupInfo classScope = groupInfo.classScopes.get(className);
		if (classScope != null) {
			scopes.add(classScope);
		}

		String packageName = getPackageName(className);
		if (packageName != null) {
			ScopedGroupInfo packageScope = groupInfo.packageScopes.get(packageName);
			if (packageScope != null) {
				scopes.add(packageScope);
			}
		}

		scopes.add(groupInfo.globalScope);
		return scopes;
	}

	@Nullable
	private static String getPackageName(String className) {
		int dotIndex = className.lastIndexOf('.');
		return dotIndex == -1 ? null : className.substring(0, dotIndex);
	}
}

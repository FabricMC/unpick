package daomephsta.unpick.impl.constantmappers.datadriven;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.constantgroupers.ConstantGroup;
import daomephsta.unpick.api.constantgroupers.IConstantGrouper;
import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.constantmappers.datadriven.parser.MemberKey;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Reader;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupConstant;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupDefinition;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupFormat;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupScope;
import daomephsta.unpick.constantmappers.datadriven.tree.GroupType;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetField;
import daomephsta.unpick.constantmappers.datadriven.tree.TargetMethod;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.BinaryExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.CastExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionTransformer;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.ExpressionVisitor;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.FieldExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.LiteralExpression;
import daomephsta.unpick.constantmappers.datadriven.tree.expr.UnaryExpression;
import daomephsta.unpick.impl.AbstractInsnNodes;
import daomephsta.unpick.impl.InstructionFactory;
import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.constantmappers.datadriven.parser.V1Parser;
import daomephsta.unpick.impl.constantmappers.datadriven.parser.v2.V2Parser;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Maps inlined values to constants using mappings defined in a file
 * @author Daomephsta
 */
public class DataDrivenConstantGrouper implements IConstantGrouper
{
	private static final Logger LOGGER = Logger.getLogger("unpick");

	private final Data data = new Data();
	private final ConstantGroup defaultGroup = new ConstantGroup("<default>", this::replaceDefault);

	public DataDrivenConstantGrouper(IConstantResolver constantResolver, InputStream... mappingSources)
	{
		for (InputStream mappingSource : mappingSources)
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(mappingSource, StandardCharsets.UTF_8));
			try
			{
				reader.mark(11);
				String versionHeader = reader.readLine();
				reader.reset();

				switch (versionHeader)
				{
				case "v1":
					V1Parser.parse(reader, constantResolver, data);
					break;

				case "v2":
					V2Parser.parse(reader, constantResolver, data);
					break;

				case "unpick v3":
					new UnpickV3Reader(reader).accept(data);
					break;

				default:
					throw new UnpickSyntaxException(1, "Unknown version or missing version header: " + versionHeader);
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		LOGGER.info(() -> String.format("Loaded %d constant groups, %d target fields and %d target methods", data.defaultGroups.size() + data.groups.size(), data.targetFields.size(), data.targetMethods.size()));
	}

	@VisibleForTesting
	public DataDrivenConstantGrouper(Consumer<UnpickV3Visitor> dataProvider)
	{
		dataProvider.accept(data);
	}

	@Override
	@Nullable
	public ConstantGroup getFieldGroup(String fieldOwner, String fieldName, String fieldDescriptor)
	{
		TargetField targetField = data.targetFields.get(new MemberKey(fieldOwner.replace('/', '.'), fieldName, fieldDescriptor));
		return targetField == null ? null : getGroupByName(targetField.groupName);
	}

	@Override
	@Nullable
	public ConstantGroup getMethodReturnGroup(String methodOwner, String methodName, String methodDescriptor)
	{
		TargetMethod targetMethod = data.targetMethods.get(new MemberKey(methodOwner.replace('/', '.'), methodName, methodDescriptor));
		return targetMethod == null || targetMethod.returnGroup == null ? null : getGroupByName(targetMethod.returnGroup);
	}

	@Override
	@Nullable
	public ConstantGroup getMethodParameterGroup(String methodOwner, String methodName, String methodDescriptor, int parameterIndex)
	{
		TargetMethod targetMethod = data.targetMethods.get(new MemberKey(methodOwner.replace('/', '.'), methodName, methodDescriptor));
		if (targetMethod == null)
		{
			return null;
		}
		String groupName = targetMethod.paramGroups.get(parameterIndex);
		return groupName == null ? null : getGroupByName(groupName);
	}

	@Override
	public ConstantGroup getDefaultGroup()
	{
		return defaultGroup;
	}

	private ConstantGroup getGroupByName(String name)
	{
		GroupInfo groupInfo = data.groups.get(name);
		if (groupInfo == null)
		{
			return null;
		}

		return new ConstantGroup(name, context -> replaceWithGroup(context, groupInfo));
	}

	private void replaceDefault(IReplacementGenerator.IContext context)
	{
		AbstractInsnNode target = context.getTarget();
		if (!AbstractInsnNodes.hasLiteralValue(target))
		{
			return;
		}

		Object literal = AbstractInsnNodes.getLiteralValue(target);
		DataType literalType;
		List<DataType> compatibleTypes;
		if (literal instanceof Integer)
		{
			literalType = DataType.INT;
			compatibleTypes = Collections.singletonList(DataType.INT);
		}
		else if (literal instanceof Long)
		{
			literalType = DataType.LONG;
			compatibleTypes = Arrays.asList(DataType.LONG, DataType.INT);
		}
		else if (literal instanceof Float)
		{
			literalType = DataType.FLOAT;
			compatibleTypes = Arrays.asList(DataType.FLOAT, DataType.LONG, DataType.INT);
		}
		else if (literal instanceof Double)
		{
			literalType = DataType.DOUBLE;
			compatibleTypes = Arrays.asList(DataType.DOUBLE, DataType.FLOAT, DataType.LONG, DataType.INT);
		}
		else if (literal instanceof String)
		{
			literalType = DataType.STRING;
			compatibleTypes = Collections.singletonList(DataType.STRING);
		}
		else
		{
			return;
		}

		for (DataType compatibleType : compatibleTypes)
		{
			Object castedLiteral = castLiteral(literal, toConstantKeyType(compatibleType));
			if (castedLiteral == null)
			{
				continue;
			}

			GroupInfo defaultGroup = data.defaultGroups.get(compatibleType);
			if (defaultGroup == null)
			{
				continue;
			}

			for (ScopedGroupInfo scope : findMatchingScopes(context, defaultGroup))
			{
				ConstantReplacementInfo replacementInfo = scope.constantReplacementMap.get(castedLiteral);
				if (!replacementInfo.strict || compatibleType == literalType)
				{
					DataType narrowedLiteralType = getNarrowedLiteralType(context, target, literalType, literal);
					replaceWithExpression(context, defaultGroup, replacementInfo.replacementExpression, narrowedLiteralType);
					return;
				}
			}
		}
	}

	private void replaceWithGroup(IReplacementGenerator.IContext context, GroupInfo groupInfo)
	{
		AbstractInsnNode target = context.getTarget();
		if (!AbstractInsnNodes.hasLiteralValue(target))
		{
			return;
		}

		Object literal = AbstractInsnNodes.getLiteralValue(target);
		DataType literalType;
		if (literal instanceof Integer)
		{
			literalType = DataType.INT;
			if (groupInfo.dataType != DataType.INT)
			{
				return;
			}
		}
		else if (literal instanceof Long)
		{
			literalType = DataType.LONG;
			if (groupInfo.dataType != DataType.INT && groupInfo.dataType != DataType.LONG)
			{
				return;
			}
		}
		else if (literal instanceof Float)
		{
			literalType = DataType.FLOAT;
			if (groupInfo.dataType != DataType.INT && groupInfo.dataType != DataType.LONG && groupInfo.dataType != DataType.FLOAT)
			{
				return;
			}
		}
		else if (literal instanceof Double)
		{
			literalType = DataType.DOUBLE;
			if (groupInfo.dataType != DataType.INT && groupInfo.dataType != DataType.LONG && groupInfo.dataType != DataType.FLOAT && groupInfo.dataType != DataType.DOUBLE)
			{
				return;
			}
		}
		else if (literal instanceof String)
		{
			literalType = DataType.STRING;
			if (groupInfo.dataType != DataType.STRING)
			{
				return;
			}
		}
		else
		{
			return;
		}

		Object castedLiteral = castLiteral(literal, toConstantKeyType(literalType));
		if (castedLiteral == null)
		{
			return;
		}

		if (groupInfo.groupType == GroupType.FLAG && !castedLiteral.equals(0L) && !castedLiteral.equals(-1L))
		{
			DataType narrowedLiteralType = getNarrowedLiteralType(context, target, literalType, literal);
			long mask;
			switch (narrowedLiteralType)
			{
				case BYTE:
					mask = 0xff;
					break;
				case SHORT:
				case CHAR:
					mask = 0xffff;
					break;
				case INT:
					mask = 0xffff_ffffL;
					break;
				case LONG:
					mask = 0xffff_ffff_ffff_ffffL;
					break;
				default:
					throw new AssertionError("Invalid literal type for  flag: " + narrowedLiteralType);
			}
			long targetValue = (Long) castedLiteral & mask;

			Map<Long, Expression> inScopeFlags = new LinkedHashMap<>();
			for (ScopedGroupInfo scope : findMatchingScopes(context, groupInfo))
			{
				scope.constantReplacementMap.forEach((key, replacementInfo) ->
				{
					if (!replacementInfo.strict || literalType == groupInfo.dataType)
					{
						inScopeFlags.putIfAbsent((Long) key & mask, replacementInfo.replacementExpression);
					}
				});
			}

			List<Map.Entry<Long, Expression>> flagList = new ArrayList<>(inScopeFlags.entrySet());
			flagList.sort(Map.Entry.comparingByKey(Comparator.comparingInt(Long::bitCount).reversed()));

			List<Expression> positiveSet = new ArrayList<>();
			long residual = targetValue;
			for (Map.Entry<Long, Expression> flag : flagList)
			{
				// make sure we aren't setting any bits that aren't set in the original value
				if ((flag.getKey() & ~targetValue) != 0)
				{
					continue;
				}
				// make sure we are actually contributing more set bits
				if ((flag.getKey() & residual) == 0)
				{
					continue;
				}

				residual &= ~flag.getKey();
				positiveSet.add(flag.getValue());
			}

			List<Expression> negativeSet = new ArrayList<>();
			long inverseTarget = targetValue ^ mask;
			long inverseResidual = inverseTarget;
			for (Map.Entry<Long, Expression> flag : flagList)
			{
				// make sure we aren't setting any bits that aren't set in the inverse value
				if ((flag.getKey() & ~inverseTarget) != 0)
				{
					continue;
				}
				// make sure we are actually contributing more set bits
				if ((flag.getKey() & inverseResidual) == 0)
				{
					continue;
				}

				inverseResidual &= ~flag.getKey();
				negativeSet.add(flag.getValue());
			}

			if (inverseResidual == 0 && (residual != 0 || negativeSet.size() < positiveSet.size()))
			{
				// negativeSet shouldn't be empty if inverseResidual is 0
				Expression oredFlags = negativeSet.get(0);
				if (literalType == DataType.INT && getExpressionType(groupInfo, oredFlags) == DataType.LONG)
				{
					oredFlags = new CastExpression(narrowedLiteralType, oredFlags);
				}
				for (int i = 1; i < negativeSet.size(); i++)
				{
					Expression nextExpr = negativeSet.get(i);
					if (literalType == DataType.INT && getExpressionType(groupInfo, nextExpr) == DataType.LONG)
					{
						nextExpr = new CastExpression(narrowedLiteralType, nextExpr);
					}
					oredFlags = new BinaryExpression(oredFlags, nextExpr, BinaryExpression.Operator.BIT_OR);
				}
				oredFlags = new UnaryExpression(oredFlags, UnaryExpression.Operator.BIT_NOT);
				replaceWithExpression(context, groupInfo, oredFlags, narrowedLiteralType);
			}
			else if (!positiveSet.isEmpty())
			{
				Expression oredFlags = positiveSet.get(0);
				if (literalType == DataType.INT && getExpressionType(groupInfo, oredFlags) == DataType.LONG)
				{
					oredFlags = new CastExpression(narrowedLiteralType, oredFlags);
				}
				for (int i = 1; i < positiveSet.size(); i++)
				{
					Expression nextExpr = positiveSet.get(i);
					if (literalType == DataType.INT && getExpressionType(groupInfo, nextExpr) == DataType.LONG)
					{
						nextExpr = new CastExpression(narrowedLiteralType, nextExpr);
					}
					oredFlags = new BinaryExpression(oredFlags, nextExpr, BinaryExpression.Operator.BIT_OR);
				}
				if (residual != 0)
				{
					Literal residualLiteral  = literalType == DataType.INT ? new Literal.Integer((int) residual) : new Literal.Long(residual);
					oredFlags = new BinaryExpression(oredFlags, new LiteralExpression(residualLiteral), BinaryExpression.Operator.BIT_OR);
				}
				replaceWithExpression(context, groupInfo, oredFlags, narrowedLiteralType);
			}
		}
		else
		{
			for (ScopedGroupInfo scope : findMatchingScopes(context, groupInfo))
			{
				ConstantReplacementInfo replacementInfo = scope.constantReplacementMap.get(castedLiteral);
				if (replacementInfo != null && (!replacementInfo.strict || literalType == groupInfo.dataType))
				{
					DataType narrowedLiteralType = getNarrowedLiteralType(context, target, literalType, literal);
					replaceWithExpression(context, groupInfo, replacementInfo.replacementExpression, narrowedLiteralType);
					return;
				}
			}
		}
	}

	private static DataType getNarrowedLiteralType(IReplacementGenerator.IContext context, AbstractInsnNode target, DataType literalType, Object literal)
	{
		if (literalType != DataType.INT)
		{
			return literalType;
		}

		int value = (Integer) literal;

		AbstractInsnNode nextInsn = AbstractInsnNodes.nextInstruction(target);
		if (nextInsn == null)
			return  literalType;
		Frame<IReplacementGenerator.IDataflowValue> dataflowFrame = context.getDataflowFrame(nextInsn);
		if (dataflowFrame == null)
			return literalType;
		Set<DataType> narrowTypeInterpretations = dataflowFrame.getStack(dataflowFrame.getStackSize() - 1).getNarrowTypeInterpretations();

		if (narrowTypeInterpretations.contains(DataType.BYTE) && value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
		{
			return DataType.BYTE;
		}
		else if (narrowTypeInterpretations.contains(DataType.SHORT) && value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
		{
			return DataType.SHORT;
		}
		else if (narrowTypeInterpretations.contains(DataType.CHAR) && value >= Character.MIN_VALUE && value <= Character.MAX_VALUE)
		{
			return DataType.CHAR;
		}
		else
		{
			return literalType;
		}
	}

	private static List<ScopedGroupInfo> findMatchingScopes(IReplacementGenerator.IContext context, GroupInfo groupInfo)
	{
		List<ScopedGroupInfo> scopes = new ArrayList<>(1);

		String className = context.getContainingClass().name.replace('/', '.');
		MethodNode method = context.getContainingMethod();

		ScopedGroupInfo methodScope = groupInfo.methodScopes.get(new MemberKey(className, method.name, method.desc));
		if (methodScope != null)
		{
			scopes.add(methodScope);
		}

		ScopedGroupInfo classScope = groupInfo.classScopes.get(className);
		if (classScope != null)
		{
			scopes.add(classScope);
		}

		String packageName = getPackageName(className);
		if (packageName != null)
		{
			ScopedGroupInfo packageScope = groupInfo.packageScopes.get(packageName);
			if (packageScope != null)
			{
				scopes.add(packageScope);
			}
		}

		scopes.add(groupInfo.globalScope);
		return scopes;
	}

	private static void replaceWithExpression(IReplacementGenerator.IContext context, GroupInfo groupInfo, Expression replacement, DataType literalType)
	{
		AbstractInsnNode targetInsn = context.getTarget();

		// check for lonesome instance field replacement to replace a null check
		if (replacement instanceof FieldExpression && !((FieldExpression) replacement).isStatic)
		{
			FieldExpression fieldReplacement = (FieldExpression) replacement;
			String fieldOwner = fieldReplacement.className.replace('.', '/');
			DataType fieldType = fieldReplacement.fieldType == null ? groupInfo.dataType : fieldReplacement.fieldType;

			AbstractInsnNode nullCheckEnd = AbstractInsnNodes.previousInstruction(targetInsn);
			if (nullCheckEnd != null)
			{
				AbstractInsnNode nullCheckStart = findStartOfNullCheck(nullCheckEnd, context.getContainingClass().version);
				if (nullCheckStart != null)
				{
					Frame<IReplacementGenerator.IDataflowValue> dataflowFrame = context.getDataflowFrame(nullCheckStart);
					if (dataflowFrame != null)
					{
						Type topOfStack = dataflowFrame.getStack(dataflowFrame.getStackSize() - 1).getDataType();
						if (topOfStack.getSort() == Type.OBJECT && context.getInheritanceChecker().isAssignableFrom(fieldOwner, topOfStack.getInternalName()))
						{
							// delete the null check
							for (AbstractInsnNode insn = nullCheckStart; insn != nullCheckEnd; insn = insn.getNext())
							{
								context.getReplacementSet().addReplacement(insn, new InsnList());
							}
							context.getReplacementSet().addReplacement(nullCheckEnd, new InsnList());

							// replace the constant instruction with a getfield
							context.getReplacementSet().addReplacement(targetInsn, new FieldInsnNode(Opcodes.GETFIELD, fieldOwner, fieldReplacement.fieldName, getDescriptor(fieldType)));
							return;
						}
					}
				}
			}
		}

		List<FieldExpression> instanceFieldExpressions = new ArrayList<>();
		replacement.accept(new ExpressionVisitor()
		{
			@Override
			public void visitFieldExpression(FieldExpression fieldExpression)
			{
				if (!fieldExpression.isStatic)
				{
					instanceFieldExpressions.add(fieldExpression);
				}
			}
		});

		if ((context.getContainingMethod().access & Opcodes.ACC_STATIC) != 0 && !instanceFieldExpressions.isEmpty())
		{
			return;
		}

		Map<FieldExpression, List<FieldNode>> thisReferenceChains = new HashMap<>();
		for (FieldExpression instanceFieldExpression : instanceFieldExpressions)
		{
			List<FieldNode> thisReferenceChain = getThisReferenceChain(context, instanceFieldExpression);
			if (thisReferenceChain == null)
			{
				return;
			}
			thisReferenceChains.put(instanceFieldExpression, thisReferenceChain);
		}

		replacement = propagateExpectedTypeDown(groupInfo, replacement, literalType);

		InsnList replacementInsns = new InsnList();
		replacement.accept(new ExpressionVisitor()
		{
			@Override
			public void visitBinaryExpression(BinaryExpression binaryExpression)
			{
				DataType leftType = getExpressionType(groupInfo, binaryExpression.lhs);
				DataType rightType = getExpressionType(groupInfo, binaryExpression.rhs);
				DataType overallType = getBinaryExpressionType(leftType, rightType);

				if (overallType == DataType.STRING)
				{
					buildStringConcatenation(context, binaryExpression);
					return;
				}

				binaryExpression.lhs.accept(this);
				addCastInsns(replacementInsns, leftType, overallType);

				binaryExpression.rhs.accept(this);
				boolean isShift = binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_LEFT || binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_RIGHT || binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED;
				addCastInsns(replacementInsns, rightType, isShift ? DataType.INT : overallType);

				int opcode;
				switch (binaryExpression.operator)
				{
					case BIT_OR:
						opcode = getOpcode(restrictToIntegralType(overallType), Opcodes.IOR);
						break;
					case BIT_XOR:
						opcode = getOpcode(restrictToIntegralType(overallType), Opcodes.IXOR);
						break;
					case BIT_AND:
						opcode = getOpcode(restrictToIntegralType(overallType), Opcodes.IAND);
						break;
					case BIT_SHIFT_LEFT:
						opcode = getOpcode(restrictToIntegralType(overallType), Opcodes.ISHL);
						break;
					case BIT_SHIFT_RIGHT:
						opcode = getOpcode(restrictToIntegralType(overallType), Opcodes.ISHR);
						break;
					case BIT_SHIFT_RIGHT_UNSIGNED:
						opcode = getOpcode(restrictToIntegralType(overallType), Opcodes.IUSHR);
						break;
					case ADD:
						opcode = getOpcode(restrictToNumberType(overallType), Opcodes.IADD);
						break;
					case SUBTRACT:
						opcode = getOpcode(restrictToNumberType(overallType), Opcodes.ISUB);
						break;
					case MULTIPLY:
						opcode = getOpcode(restrictToNumberType(overallType), Opcodes.IMUL);
						break;
					case DIVIDE:
						opcode = getOpcode(restrictToNumberType(overallType), Opcodes.IDIV);
						break;
					case MODULO:
						opcode = getOpcode(restrictToNumberType(overallType), Opcodes.IREM);
						break;
					default:
						throw new AssertionError("Unknown binary operator: " + binaryExpression.operator);
				}

				replacementInsns.add(new InsnNode(opcode));
			}

			@Override
			public void visitCastExpression(CastExpression castExpression)
			{
				castExpression.operand.accept(this);

				DataType operandType = getExpressionType(groupInfo, castExpression.operand);
				addCastInsns(replacementInsns, operandType, castExpression.castType);
			}

			@Override
			public void visitFieldExpression(FieldExpression fieldExpression)
			{
				String fieldDesc = getDescriptor(fieldExpression.fieldType == null ? groupInfo.dataType : fieldExpression.fieldType);
				if (fieldExpression.isStatic)
				{
					String fieldOwner = fieldExpression.className.replace('.', '/');
					replacementInsns.add(new FieldInsnNode(Opcodes.GETSTATIC, fieldOwner, fieldExpression.fieldName, fieldDesc));
				}
				else
				{
					replacementInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
					String thisType = context.getContainingClass().name;
					for (FieldNode outerThisField : thisReferenceChains.get(fieldExpression))
					{
						replacementInsns.add(new FieldInsnNode(Opcodes.GETFIELD, thisType, outerThisField.name, outerThisField.desc));
						thisType = Type.getType(outerThisField.desc).getInternalName();
					}
					replacementInsns.add(new FieldInsnNode(Opcodes.GETFIELD, thisType, fieldExpression.fieldName, fieldDesc));
				}
			}

			@Override
			public void visitLiteralExpression(LiteralExpression literalExpression)
			{
				replacementInsns.add(InstructionFactory.pushValue(literalToObject(literalExpression.literal)));
			}

			@Override
			public void visitUnaryExpression(UnaryExpression unaryExpression)
			{
				if (unaryExpression.operator == UnaryExpression.Operator.NEGATE && unaryExpression.operand instanceof LiteralExpression)
				{
					Literal literal = ((LiteralExpression) unaryExpression.operand).literal;
					if (literal instanceof Literal.Integer)
					{
						replacementInsns.add(InstructionFactory.pushInt(-((Literal.Integer) literal).value));
						return;
					}
					else if (literal instanceof Literal.Long)
					{
						replacementInsns.add(InstructionFactory.pushLong(-((Literal.Long) literal).value));
						return;
					}
					else if (literal instanceof Literal.Float)
					{
						replacementInsns.add(InstructionFactory.pushFloat(-((Literal.Float) literal).value));
						return;
					}
					else if (literal instanceof Literal.Double)
					{
						replacementInsns.add(InstructionFactory.pushDouble(-((Literal.Double) literal).value));
						return;
					}
				}

				unaryExpression.operand.accept(this);

				DataType operandType = getExpressionType(groupInfo, unaryExpression.operand);
				switch (unaryExpression.operator)
				{
					case NEGATE:
						replacementInsns.add(new InsnNode(getOpcode(restrictToNumberType(operandType), Opcodes.INEG)));
						break;
					case BIT_NOT:
						if (restrictToIntegralType(operandType) == DataType.INT)
						{
							replacementInsns.add(InstructionFactory.pushInt(0xffff_ffff));
							replacementInsns.add(new InsnNode(Opcodes.IXOR));
						}
						else
						{
							replacementInsns.add(InstructionFactory.pushLong(0xffff_ffff_ffff_ffffL));
							replacementInsns.add(new InsnNode(Opcodes.LXOR));
						}
						break;
					default:
						throw new AssertionError("Unknown unary operator: " + unaryExpression.operator);
				}
			}

			private void buildStringConcatenation(IReplacementGenerator.IContext context, BinaryExpression binaryExpr)
			{
				List<Expression> stuffToConcatenate = new ArrayList<>();
				while (true)
				{
					stuffToConcatenate.add(0, binaryExpr.rhs);
					if (!(binaryExpr.lhs instanceof BinaryExpression))
					{
						stuffToConcatenate.add(0, binaryExpr.lhs);
						break;
					}
					binaryExpr = (BinaryExpression) binaryExpr.lhs;
				}

				if (context.getContainingClass().version <= Opcodes.V1_8)
				{
					replacementInsns.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
					replacementInsns.add(new InsnNode(Opcodes.DUP));
					replacementInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
					for (Expression expression : stuffToConcatenate)
					{
						expression.accept(this);
						String sbDesc = "(" + getDescriptor(getExpressionType(groupInfo, expression)) + ")Ljava/lang/StringBuilder;";
						replacementInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", sbDesc, false));
					}
					replacementInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
				}
				else
				{
					StringBuilder recipe = new StringBuilder(stuffToConcatenate.size());
					StringBuilder concatType = new StringBuilder("(");
					for (Expression expression : stuffToConcatenate)
					{
						recipe.append('\1');
						concatType.append(getDescriptor(getExpressionType(groupInfo, expression)));
						expression.accept(this);
					}
					concatType.append(")Ljava/lang/String;");
					replacementInsns.add(new InvokeDynamicInsnNode(
						"makeConcatWithConstants",
						concatType.toString(),
						new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/StringConcatFactory", "makeConcatWithConstants", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false),
						recipe.toString()
					));
				}
			}

			private DataType restrictToIntegralType(DataType dataType)
			{
				if (dataType == DataType.BYTE || dataType == DataType.SHORT || dataType == DataType.CHAR || dataType == DataType.INT)
				{
					return DataType.INT;
				}
				else
				{
					return DataType.LONG;
				}
			}

			private DataType restrictToNumberType(DataType dataType)
			{
				switch (dataType)
				{
					case BYTE:
					case SHORT:
					case CHAR:
					case INT:
						return DataType.INT;
					case LONG:
						return DataType.LONG;
					case FLOAT:
						return DataType.FLOAT;
					default:
						return DataType.DOUBLE;
				}
			}
		});

		addCastInsns(replacementInsns, getExpressionType(groupInfo, replacement), literalType);

		context.getReplacementSet().addReplacement(targetInsn, replacementInsns);
	}

	private static Expression propagateExpectedTypeDown(GroupInfo groupInfo, Expression expression, DataType expectedType)
	{
		return expression.transform(new ExpressionTransformer()
		{
			private DataType myExpectedType = expectedType;

			@Override
			public Expression transformBinaryExpression(BinaryExpression binaryExpression)
			{
				if (myExpectedType == DataType.STRING)
				{
					// don't transform other stuff into strings (changes semantics)
					return binaryExpression;
				}

				boolean isShift = binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_LEFT || binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_RIGHT || binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED;
				DataType leftType = getExpressionType(groupInfo, binaryExpression.lhs);
				DataType rightType = getExpressionType(groupInfo, binaryExpression.rhs);
				DataType overallType = getBinaryExpressionType(getBinaryExpressionType(leftType, rightType), myExpectedType);

				myExpectedType = overallType;
				Expression lhs = binaryExpression.lhs.transform(this);
				myExpectedType = isShift ? DataType.INT : overallType;
				Expression rhs = binaryExpression.rhs.transform(this);
				return new BinaryExpression(lhs, rhs, binaryExpression.operator);
			}

			@Override
			public Expression transformCastExpression(CastExpression castExpression)
			{
				myExpectedType = castExpression.castType;
				return new CastExpression(castExpression.castType, castExpression.operand.transform(this));
			}

			@Override
			public Expression transformLiteralExpression(LiteralExpression literalExpression)
			{
				return new LiteralExpression(objectToLiteral(castLiteral(literalToObject(literalExpression.literal), myExpectedType)));
			}
		});
	}

	private static int getOpcode(DataType dataType, int intOpcode)
	{
		Type type;
		switch (dataType)
		{
			case BYTE:
			case SHORT:
			case CHAR:
			case INT:
				type = Type.INT_TYPE;
				break;
			case LONG:
				type = Type.LONG_TYPE;
				break;
			case FLOAT:
				type = Type.FLOAT_TYPE;
				break;
			case DOUBLE:
				type = Type.DOUBLE_TYPE;
				break;
			case STRING:
				type = Type.getObjectType("java/lang/String");
				break;
			default:
				throw new AssertionError("Unexpected type: " + dataType);
		}

		return type.getOpcode(intOpcode);
	}

	private static void addCastInsns(InsnList insns, DataType fromType, DataType toType)
	{
		int opcode;
		switch (fromType)
		{
			case BYTE:
			case SHORT:
			case CHAR:
			case INT:
				switch (toType)
				{
					case BYTE:
						if (fromType != DataType.BYTE)
						{
							insns.add(new InsnNode(Opcodes.I2B));
						}
						return;
					case SHORT:
						if (fromType != DataType.BYTE && fromType != DataType.SHORT)
						{
							insns.add(new InsnNode(Opcodes.I2S));
						}
						return;
					case CHAR:
						if (fromType != DataType.CHAR)
						{
							insns.add(new InsnNode(Opcodes.I2C));
						}
						return;
					case LONG:
						opcode = Opcodes.I2L;
						break;
					case FLOAT:
						opcode = Opcodes.I2F;
						break;
					case DOUBLE:
						opcode = Opcodes.I2D;
						break;
					default:
						return;
				}
				break;
			case LONG:
				switch (toType)
				{
					case BYTE:
					case SHORT:
					case CHAR:
					case INT:
						opcode = Opcodes.L2I;
						break;
					case FLOAT:
						opcode = Opcodes.L2F;
						break;
					case DOUBLE:
						opcode = Opcodes.L2D;
						break;
					default:
						return;
				}
				break;
			case FLOAT:
				switch (toType)
				{
					case BYTE:
					case SHORT:
					case CHAR:
					case INT:
						opcode = Opcodes.F2I;
						break;
					case LONG:
						opcode = Opcodes.F2L;
						break;
					case DOUBLE:
						opcode = Opcodes.F2D;
						break;
					default:
						return;
				}
				break;
			case DOUBLE:
				switch (toType)
				{
					case BYTE:
					case SHORT:
					case CHAR:
					case INT:
						opcode = Opcodes.D2I;
						break;
					case LONG:
						opcode = Opcodes.D2L;
						break;
					case FLOAT:
						opcode = Opcodes.D2F;
						break;
					default:
						return;
				}
				break;
			default:
				return;
		}

		insns.add(new InsnNode(opcode));
		switch (toType)
		{
			case BYTE:
				insns.add(new InsnNode(Opcodes.I2B));
				break;
			case SHORT:
				insns.add(new InsnNode(Opcodes.I2S));
				break;
			case CHAR:
				insns.add(new InsnNode(Opcodes.I2C));
				break;
		}
	}

	private static AbstractInsnNode findStartOfNullCheck(AbstractInsnNode insn, int classVersion)
	{
		if (insn.getOpcode() != Opcodes.POP)
		{
			return null;
		}

		insn = AbstractInsnNodes.previousInstruction(insn);

		if (insn == null || insn.getType() != AbstractInsnNode.METHOD_INSN)
		{
			return null;
		}

		MethodInsnNode methodInsn = (MethodInsnNode) insn;

		boolean isNullCheck;
		if (classVersion <= Opcodes.V1_8)
		{
			isNullCheck = methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL &&
				"getClass".equals(methodInsn.name) &&
				"()Ljava/lang/Class;".equals(methodInsn.desc);
		}
		else
		{
			isNullCheck = methodInsn.getOpcode() == Opcodes.INVOKESTATIC &&
				"java/util/Objects".equals(methodInsn.owner) &&
				"requireNonNull".equals(methodInsn.name) &&
				"(Ljava/lang/Object;)Ljava/lang/Object;".equals(methodInsn.desc);
		}

		return isNullCheck ? insn : null;
	}

	private static DataType getBinaryExpressionType(DataType leftType, DataType rightType)
	{
		if (leftType == DataType.STRING || rightType == DataType.STRING)
		{
			return DataType.STRING;
		}
		else if (leftType == DataType.DOUBLE || rightType == DataType.DOUBLE)
		{
			return DataType.DOUBLE;
		}
		else if (leftType == DataType.FLOAT || rightType == DataType.FLOAT)
		{
			return DataType.FLOAT;
		}
		else if (leftType == DataType.LONG || rightType == DataType.LONG)
		{
			return DataType.LONG;
		}
		else
		{
			return DataType.INT;
		}
	}

	private static DataType getUnaryExpressionType(DataType operandType)
	{
		if (operandType == DataType.BYTE || operandType == DataType.SHORT || operandType == DataType.CHAR)
		{
			return DataType.INT;
		}
		else
		{
			return operandType;
		}
	}

	private static DataType getExpressionType(GroupInfo groupInfo, Expression expression)
	{
		DataType[] result = {null};
		expression.accept(new ExpressionVisitor()
		{
			@Override
			public void visitBinaryExpression(BinaryExpression binaryExpression)
			{
				binaryExpression.lhs.accept(this);
				DataType leftSide = result[0];
				binaryExpression.rhs.accept(this);
				DataType rightSide = result[0];
				result[0] = getBinaryExpressionType(leftSide, rightSide);
			}

			@Override
			public void visitCastExpression(CastExpression castExpression)
			{
				result[0] = castExpression.castType;
			}

			@Override
			public void visitFieldExpression(FieldExpression fieldExpression)
			{
				result[0] = fieldExpression.fieldType;
				if (result[0] == null)
				{
					result[0] = groupInfo.dataType;
				}
			}

			@Override
			public void visitLiteralExpression(LiteralExpression literalExpression)
			{
				Literal literal = literalExpression.literal;
				if (literal instanceof Literal.Integer)
				{
					result[0] = DataType.INT;
				}
				else if (literal instanceof Literal.Long)
				{
					result[0] = DataType.LONG;
				}
				else if (literal instanceof Literal.Float)
				{
					result[0] = DataType.FLOAT;
				}
				else if (literal instanceof Literal.Double)
				{
					result[0] = DataType.DOUBLE;
				}
				else if (literal instanceof Literal.String)
				{
					result[0] = DataType.STRING;
				}
				else if (literal instanceof Literal.Character)
				{
					result[0] = DataType.CHAR;
				}
				else
				{
					throw new AssertionError("Unexpected literal type: " + literal.getClass().getName());
				}
			}

			@Override
			public void visitUnaryExpression(UnaryExpression unaryExpression)
			{
				super.visitUnaryExpression(unaryExpression);
				result[0] = getUnaryExpressionType(result[0]);
			}
		});

		return result[0];
	}

	@Nullable
	private static String getPackageName(String className)
	{
		int dotIndex = className.lastIndexOf('.');
		return dotIndex == -1 ? null : className.substring(0, dotIndex);
	}

	@Nullable
	private static Object castLiteral(Object literal, DataType destType)
	{
		switch (destType)
		{
			case BYTE:
			case SHORT:
			case CHAR:
			case INT:
			{
				int result = ((Number) literal).intValue();
				boolean exactCast = result == ((Number) literal).doubleValue();
				return exactCast ? result : null;
			}
			case LONG:
			{
				long result = ((Number) literal).longValue();
				boolean exactCast = !Utils.isFloatingPoint(literal) || result == ((Number) literal).doubleValue();
				return exactCast ? result : null;
			}
			case FLOAT:
			{
				float result = ((Number) literal).floatValue();
				boolean exactCast = Utils.isFloatingPoint(literal) ? Double.compare(result, ((Number) literal).doubleValue()) == 0 : (long) result == ((Number) literal).longValue();
				return exactCast ? result : null;
			}
			case DOUBLE:
			{
				double result = ((Number) literal).doubleValue();
				boolean exactCast = Utils.isFloatingPoint(literal) || (long) result == ((Number) literal).longValue();
				return exactCast ? result : null;
			}
			case STRING:
				return literal;
			default:
				throw new AssertionError("Unknown literal data type " + destType);
		}
	}

	private static Object literalToObject(Literal literal)
	{
		if (literal instanceof Literal.Integer)
		{
			return ((Literal.Integer) literal).value;
		}
		else if (literal instanceof Literal.Long)
		{
			return ((Literal.Long) literal).value;
		}
		else if (literal instanceof Literal.Float)
		{
			return ((Literal.Float) literal).value;
		}
		else if (literal instanceof Literal.Double)
		{
			return ((Literal.Double) literal).value;
		}
		else if (literal instanceof Literal.Character)
		{
			return ((Literal.Character) literal).value;
		}
		else if (literal instanceof Literal.String)
		{
			return ((Literal.String) literal).value;
		}
		else
		{
			throw new AssertionError("Unexpected literal type: " + literal.getClass().getName());
		}
	}

	private static Literal objectToLiteral(Object object)
	{
		if (object instanceof Integer)
		{
			return new Literal.Integer((Integer) object);
		}
		else if (object instanceof Long)
		{
			return new Literal.Long((Long) object);
		}
		else if (object instanceof Float)
		{
			return new Literal.Float((Float) object);
		}
		else if (object instanceof Double)
		{
			return new Literal.Double((Double) object);
		}
		else if (object instanceof Character)
		{
			return new Literal.Character((Character) object);
		}
		else if (object instanceof String)
		{
			return new Literal.String((String) object);
		}
		else
		{
			throw new AssertionError("Unexpected literal type: " + object.getClass().getName());
		}
	}

	private static DataType toConstantKeyType(DataType dataType)
	{
		switch (dataType)
		{
			case BYTE:
			case SHORT:
			case CHAR:
			case INT:
			case LONG:
				return DataType.LONG;
			case FLOAT:
			case DOUBLE:
				return DataType.DOUBLE;
			case STRING:
				return DataType.STRING;
			default:
				throw new AssertionError("Unsupported data type " + dataType);
		}
	}

	private static String getDescriptor(DataType dataType)
	{
		switch (dataType)
		{
			case BYTE:
				return "B";
			case SHORT:
				return "S";
			case INT:
				return "I";
			case LONG:
				return "J";
			case FLOAT:
				return "F";
			case DOUBLE:
				return "D";
			case CHAR:
				return "C";
			case STRING:
				return "Ljava/lang/String;";
			default:
				throw new AssertionError("Unsupported data type " + dataType);
		}
	}

	@Nullable
	private static OuterClassReference getOuterClassReference(IReplacementGenerator.IContext context, ClassNode innerClass)
	{
		if (innerClass.fields == null || innerClass.fields.isEmpty())
		{
			return null;
		}

		int slashIndex = innerClass.name.lastIndexOf('/');
		int dollarIndex = innerClass.name.lastIndexOf('$');
		if (dollarIndex <= slashIndex)
		{
			return null;
		}

		String outerClassName = innerClass.name.substring(0, dollarIndex);

		if (!innerClass.fields.get(0).desc.equals("L" + outerClassName + ";"))
		{
			return null;
		}

		ClassReader reader = context.getClassResolver().resolveClass(outerClassName);
		if (reader == null)
			return null;

		ClassNode outerClass = new ClassNode();
		reader.accept(outerClass, ClassReader.SKIP_CODE);
		boolean isInnerClassStatic = true;
		if (outerClass.innerClasses != null)
		{
			for (InnerClassNode innerClassNode : outerClass.innerClasses)
			{
				if (innerClassNode.name.equals(innerClass.name))
				{
					isInnerClassStatic = (innerClassNode.access & Opcodes.ACC_STATIC) != 0;
					break;
				}
			}
		}

		if (isInnerClassStatic)
		{
			return null;
		}

		return new OuterClassReference(outerClass, innerClass.fields.get(0));
	}

	@Nullable
	private static List<FieldNode> getThisReferenceChain(IReplacementGenerator.IContext context, FieldExpression fieldExpr)
	{
		String fieldOwner = fieldExpr.className.replace('.', '/');
		ClassNode clazz = context.getContainingClass();
		List<FieldNode> thisReferenceChain = new ArrayList<>();
		while (true)
		{
			if (context.getInheritanceChecker().isAssignableFrom(fieldOwner, clazz.name))
			{
				return thisReferenceChain;
			}

			OuterClassReference outerClassRef = getOuterClassReference(context, clazz);
			if (outerClassRef == null)
			{
				return null;
			}
			thisReferenceChain.add(outerClassRef.outerThisReference);
			clazz = outerClassRef.outerClass;
		}
	}

	private static final class OuterClassReference
	{
		private final ClassNode outerClass;
		private final FieldNode outerThisReference;

		private OuterClassReference(ClassNode outerClass, FieldNode outerThisReference)
		{
			this.outerClass = outerClass;
			this.outerThisReference = outerThisReference;
		}
	}

	private static final class GroupInfo
	{
		private final DataType dataType;
		private final GroupType groupType;
		private final ScopedGroupInfo globalScope = new ScopedGroupInfo();
		private final Map<String, ScopedGroupInfo> packageScopes = new HashMap<>();
		private final Map<String, ScopedGroupInfo> classScopes = new HashMap<>();
		private final Map<MemberKey, ScopedGroupInfo> methodScopes = new HashMap<>();

		private GroupInfo(DataType dataType, GroupType groupType)
		{
			this.dataType = dataType;
			this.groupType = groupType;
		}
	}

	private static final class ScopedGroupInfo
	{
		@Nullable
		private GroupFormat format;
		private final Map<Object, ConstantReplacementInfo> constantReplacementMap = new HashMap<>();
	}

	private static final class ConstantReplacementInfo
	{
		private final boolean strict;
		private final Expression replacementExpression;

		private ConstantReplacementInfo(boolean strict, Expression replacementExpression)
		{
			this.strict = strict;
			this.replacementExpression = replacementExpression;
		}
	}

	public static final class Data extends UnpickV3Visitor
	{
		private final Map<DataType, GroupInfo> defaultGroups = new EnumMap<>(DataType.class);
		private final Map<String, GroupInfo> groups = new HashMap<>();
		private final Map<MemberKey, TargetField> targetFields = new HashMap<>();
		private final Map<MemberKey, TargetMethod> targetMethods = new HashMap<>();

		private Data()
		{
		}

		@Override
		public void visitGroupDefinition(GroupDefinition groupDefinition)
		{
			GroupInfo existingInfo = groupDefinition.name == null
				? defaultGroups.computeIfAbsent(groupDefinition.dataType, k -> new GroupInfo(groupDefinition.dataType, groupDefinition.type))
				: groups.computeIfAbsent(groupDefinition.name, k -> new GroupInfo(groupDefinition.dataType, groupDefinition.type));
			String groupDisplayName = groupDefinition.name == null ? "<default>" : groupDefinition.name;

			if (existingInfo.groupType != groupDefinition.type)
			{
				throw new UnpickSyntaxException("Group type mismatch for group " + groupDisplayName);
			}
			if (existingInfo.dataType != groupDefinition.dataType)
			{
				throw new UnpickSyntaxException("Data type mismatch for group " + groupDisplayName);
			}

			ScopedGroupInfo existingScopedInfo;
			if (groupDefinition.scope instanceof GroupScope.Global)
			{
				existingScopedInfo = existingInfo.globalScope;
			}
			else if (groupDefinition.scope instanceof GroupScope.Package)
			{
				existingScopedInfo = existingInfo.packageScopes.computeIfAbsent(((GroupScope.Package) groupDefinition.scope).packageName, k -> new ScopedGroupInfo());
			}
			else if (groupDefinition.scope instanceof GroupScope.Class)
			{
				existingScopedInfo = existingInfo.classScopes.computeIfAbsent(((GroupScope.Class) groupDefinition.scope).className, k -> new ScopedGroupInfo());
			}
			else if (groupDefinition.scope instanceof GroupScope.Method)
			{
				GroupScope.Method methodScope = (GroupScope.Method) groupDefinition.scope;
				existingScopedInfo = existingInfo.methodScopes.computeIfAbsent(new MemberKey(methodScope.className, methodScope.methodName, methodScope.methodDesc), k -> new ScopedGroupInfo());
			}
			else
			{
				throw new AssertionError("Unknown scope type: " + groupDefinition.scope.getClass().getName());
			}

			if (groupDefinition.format != null)
			{
				if (existingScopedInfo.format != groupDefinition.format && existingScopedInfo.format != null)
				{
					throw new UnpickSyntaxException("Format mismatch for group " + groupDisplayName);
				}

				existingScopedInfo.format = groupDefinition.format;
			}

			for (GroupConstant constant : groupDefinition.constants)
			{
				Object cst = extractConstantObject(constant.key, groupDefinition.dataType);

				if (existingScopedInfo.constantReplacementMap.put(cst, new ConstantReplacementInfo(groupDefinition.strict, constant.value)) != null)
				{
					throw new UnpickSyntaxException("Duplicate constant in group " + groupDisplayName + ": " + cst);
				}
			}
		}

		@Override
		public void visitTargetField(TargetField targetField)
		{
			if (targetFields.put(new MemberKey(targetField.className, targetField.fieldName, targetField.fieldDesc), targetField) != null)
			{
				throw new UnpickSyntaxException("Duplicate target field: " + targetField.className + "." + targetField.fieldName);
			}
		}

		@Override
		public void visitTargetMethod(TargetMethod targetMethod)
		{
			MemberKey key = new MemberKey(targetMethod.className, targetMethod.methodName, targetMethod.methodDesc);
			TargetMethod existingTargetMethod = targetMethods.get(key);
			if (existingTargetMethod == null)
			{
				targetMethods.put(key, targetMethod);
				return;
			}

			Map<Integer, String> paramGroups = new HashMap<>(existingTargetMethod.paramGroups);
			targetMethod.paramGroups.forEach((paramIndex, groupName) ->
			{
				if (paramGroups.put(paramIndex, groupName) != null)
				{
					throw new UnpickSyntaxException("Duplicate param group: " + targetMethod.className + "." + targetMethod.methodName + targetMethod.methodDesc + " " + paramIndex);
				}
			});

			String returnGroup;
			if (targetMethod.returnGroup != null)
			{
				if (existingTargetMethod.returnGroup != null)
				{
					throw new UnpickSyntaxException("Duplicate return group: " + targetMethod.className + "." + targetMethod.methodName + targetMethod.methodDesc);
				}
				returnGroup = targetMethod.returnGroup;
			}
			else
			{
				returnGroup = existingTargetMethod.returnGroup;
			}

			TargetMethod merged = new TargetMethod(
				targetMethod.className,
				targetMethod.methodName,
				targetMethod.methodDesc,
				paramGroups,
				returnGroup
			);
			targetMethods.put(key, merged);
		}

		private static Object extractConstantObject(Literal.ConstantKey constant, DataType groupType)
		{
			Object uncastedObject;
			if (constant instanceof Literal.Long)
			{
				uncastedObject = ((Literal.Long) constant).value;
			}
			else if (constant instanceof Literal.Double)
			{
				uncastedObject = ((Literal.Double) constant).value;
			}
			else if (constant instanceof Literal.String)
			{
				uncastedObject = ((Literal.String) constant).value;
			}
			else
			{
				throw new AssertionError("Unknown constant type: " + constant.getClass().getName());
			}

			switch (groupType)
			{
				case INT:
					assert uncastedObject instanceof Number;
					return (long) ((Number) uncastedObject).intValue();
				case LONG:
					assert uncastedObject instanceof Number;
					return ((Number) uncastedObject).longValue();
				case FLOAT:
					assert uncastedObject instanceof Number;
					return (double) ((Number) uncastedObject).floatValue();
				case DOUBLE:
					assert uncastedObject instanceof Number;
					return ((Number) uncastedObject).doubleValue();
				case STRING:
					assert uncastedObject instanceof String;
					return uncastedObject;
				default:
					throw new AssertionError("Invalid group type: " + groupType);
			}
		}
	}
}

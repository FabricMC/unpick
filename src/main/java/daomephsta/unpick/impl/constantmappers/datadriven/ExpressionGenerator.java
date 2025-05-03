package daomephsta.unpick.impl.constantmappers.datadriven;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
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
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;
import daomephsta.unpick.constantmappers.datadriven.tree.Literal;
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
import daomephsta.unpick.impl.constantmappers.datadriven.data.GroupInfo;
import daomephsta.unpick.impl.constantmappers.datadriven.data.ScopedGroupInfo;

public final class ExpressionGenerator {
	private ExpressionGenerator() {
	}

	@Nullable
	public static Expression generateFlagsExpression(IReplacementGenerator.IContext context, GroupInfo groupInfo, long targetValue, DataType literalType, DataType narrowedLiteralType) {
		long mask = switch (narrowedLiteralType) {
			case BYTE -> 0xff;
			case SHORT, CHAR -> 0xffff;
			case INT -> 0xffff_ffffL;
			case LONG -> 0xffff_ffff_ffff_ffffL;
			default -> throw new AssertionError("Invalid literal type for flag: " + narrowedLiteralType);
		};
		targetValue &= mask;

		Map<Long, Expression> inScopeFlags = new LinkedHashMap<>();
		for (ScopedGroupInfo scope : DataDrivenConstantGrouper.findMatchingScopes(context, groupInfo)) {
			scope.constantReplacementMap.forEach((key, replacementInfo) -> {
				if (!replacementInfo.strict() || literalType == groupInfo.dataType) {
					inScopeFlags.putIfAbsent((Long) key & mask, replacementInfo.replacementExpression());
				}
			});
		}

		List<Map.Entry<Long, Expression>> flagList = new ArrayList<>(inScopeFlags.entrySet());
		flagList.sort(Map.Entry.comparingByKey(Comparator.comparingInt(Long::bitCount).reversed()));

		List<Expression> positiveSet = new ArrayList<>();
		long residual = targetValue;
		for (Map.Entry<Long, Expression> flag : flagList) {
			// make sure we aren't setting any bits that aren't set in the original value
			if ((flag.getKey() & ~targetValue) != 0) {
				continue;
			}
			// make sure we are actually contributing more set bits
			if ((flag.getKey() & residual) == 0) {
				continue;
			}

			residual &= ~flag.getKey();
			positiveSet.add(flag.getValue());
		}

		List<Expression> negativeSet = new ArrayList<>();
		long inverseTarget = targetValue ^ mask;
		long inverseResidual = inverseTarget;
		for (Map.Entry<Long, Expression> flag : flagList) {
			// make sure we aren't setting any bits that aren't set in the inverse value
			if ((flag.getKey() & ~inverseTarget) != 0) {
				continue;
			}
			// make sure we are actually contributing more set bits
			if ((flag.getKey() & inverseResidual) == 0) {
				continue;
			}

			inverseResidual &= ~flag.getKey();
			negativeSet.add(flag.getValue());
		}

		if (inverseResidual == 0 && (residual != 0 || negativeSet.size() < positiveSet.size())) {
			// negativeSet shouldn't be empty if inverseResidual is 0
			Expression oredFlags = negativeSet.getFirst();
			if (literalType == DataType.INT && getExpressionType(groupInfo, oredFlags) == DataType.LONG) {
				oredFlags = new CastExpression(narrowedLiteralType, oredFlags);
			}
			for (int i = 1; i < negativeSet.size(); i++) {
				Expression nextExpr = negativeSet.get(i);
				if (literalType == DataType.INT && getExpressionType(groupInfo, nextExpr) == DataType.LONG) {
					nextExpr = new CastExpression(narrowedLiteralType, nextExpr);
				}
				oredFlags = new BinaryExpression(oredFlags, nextExpr, BinaryExpression.Operator.BIT_OR);
			}
			return new UnaryExpression(oredFlags, UnaryExpression.Operator.BIT_NOT);
		} else if (!positiveSet.isEmpty()) {
			Expression oredFlags = positiveSet.getFirst();
			if (literalType == DataType.INT && getExpressionType(groupInfo, oredFlags) == DataType.LONG) {
				oredFlags = new CastExpression(narrowedLiteralType, oredFlags);
			}
			for (int i = 1; i < positiveSet.size(); i++) {
				Expression nextExpr = positiveSet.get(i);
				if (literalType == DataType.INT && getExpressionType(groupInfo, nextExpr) == DataType.LONG) {
					nextExpr = new CastExpression(narrowedLiteralType, nextExpr);
				}
				oredFlags = new BinaryExpression(oredFlags, nextExpr, BinaryExpression.Operator.BIT_OR);
			}
			if (residual != 0) {
				Literal residualLiteral = literalType == DataType.INT ? new Literal.Integer((int) residual) : new Literal.Long(residual);
				return new BinaryExpression(oredFlags, new LiteralExpression(residualLiteral), BinaryExpression.Operator.BIT_OR);
			} else {
				return oredFlags;
			}
		} else {
			return null;
		}
	}

	public static void replaceWithExpression(IReplacementGenerator.IContext context, GroupInfo groupInfo, Expression replacement, DataType literalType) {
		AbstractInsnNode targetInsn = context.getTarget();

		// check for lonesome instance field replacement to replace a null check
		if (replacement instanceof FieldExpression fieldReplacement && !fieldReplacement.isStatic) {
			String fieldOwner = fieldReplacement.className.replace('.', '/');
			DataType fieldType = fieldReplacement.fieldType == null ? groupInfo.dataType : fieldReplacement.fieldType;

			AbstractInsnNode nullCheckEnd = AbstractInsnNodes.previousInstruction(targetInsn);
			if (nullCheckEnd != null) {
				AbstractInsnNode nullCheckStart = findStartOfNullCheck(nullCheckEnd, context.getContainingClass().version);
				if (nullCheckStart != null) {
					Frame<IReplacementGenerator.IDataflowValue> dataflowFrame = context.getDataflowFrame(nullCheckStart);
					if (dataflowFrame != null) {
						Type topOfStack = dataflowFrame.getStack(dataflowFrame.getStackSize() - 1).getDataType();
						if (topOfStack.getSort() == Type.OBJECT && context.getInheritanceChecker().isAssignableFrom(fieldOwner, topOfStack.getInternalName())) {
							// delete the null check
							for (AbstractInsnNode insn = nullCheckStart; insn != nullCheckEnd; insn = insn.getNext()) {
								context.getReplacementSet().addReplacement(insn, new InsnList());
							}
							context.getReplacementSet().addReplacement(nullCheckEnd, new InsnList());

							// replace the constant instruction with a getfield
							context.getReplacementSet().addReplacement(targetInsn, new FieldInsnNode(Opcodes.GETFIELD, fieldOwner, fieldReplacement.fieldName, Utils.getDescriptor(fieldType)));
							return;
						}
					}
				}
			}
		}

		List<FieldExpression> instanceFieldExpressions = new ArrayList<>();
		replacement.accept(new ExpressionVisitor() {
			@Override
			public void visitFieldExpression(FieldExpression fieldExpression) {
				if (!fieldExpression.isStatic) {
					instanceFieldExpressions.add(fieldExpression);
				}
			}
		});

		if ((context.getContainingMethod().access & Opcodes.ACC_STATIC) != 0 && !instanceFieldExpressions.isEmpty()) {
			return;
		}

		Map<FieldExpression, List<FieldNode>> thisReferenceChains = new HashMap<>();
		for (FieldExpression instanceFieldExpression : instanceFieldExpressions) {
			List<FieldNode> thisReferenceChain = getThisReferenceChain(context, instanceFieldExpression);
			if (thisReferenceChain == null) {
				return;
			}
			thisReferenceChains.put(instanceFieldExpression, thisReferenceChain);
		}

		replacement = propagateExpectedTypeDown(groupInfo, replacement, literalType);

		InsnList replacementInsns = new InsnList();
		replacement.accept(new ExpressionVisitor() {
			@Override
			public void visitBinaryExpression(BinaryExpression binaryExpression) {
				DataType leftType = getExpressionType(groupInfo, binaryExpression.lhs);
				DataType rightType = getExpressionType(groupInfo, binaryExpression.rhs);
				DataType overallType = getBinaryExpressionType(leftType, rightType);

				if (overallType == DataType.STRING) {
					buildStringConcatenation(context, binaryExpression);
					return;
				}

				binaryExpression.lhs.accept(this);
				addCastInsns(replacementInsns, leftType, overallType);

				binaryExpression.rhs.accept(this);
				boolean isShift = binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_LEFT || binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_RIGHT || binaryExpression.operator == BinaryExpression.Operator.BIT_SHIFT_RIGHT_UNSIGNED;
				addCastInsns(replacementInsns, rightType, isShift ? DataType.INT : overallType);

				int opcode = switch (binaryExpression.operator) {
					case BIT_OR -> getOpcode(restrictToIntegralType(overallType), Opcodes.IOR);
					case BIT_XOR -> getOpcode(restrictToIntegralType(overallType), Opcodes.IXOR);
					case BIT_AND -> getOpcode(restrictToIntegralType(overallType), Opcodes.IAND);
					case BIT_SHIFT_LEFT -> getOpcode(restrictToIntegralType(overallType), Opcodes.ISHL);
					case BIT_SHIFT_RIGHT -> getOpcode(restrictToIntegralType(overallType), Opcodes.ISHR);
					case BIT_SHIFT_RIGHT_UNSIGNED -> getOpcode(restrictToIntegralType(overallType), Opcodes.IUSHR);
					case ADD -> getOpcode(restrictToNumberType(overallType), Opcodes.IADD);
					case SUBTRACT -> getOpcode(restrictToNumberType(overallType), Opcodes.ISUB);
					case MULTIPLY -> getOpcode(restrictToNumberType(overallType), Opcodes.IMUL);
					case DIVIDE -> getOpcode(restrictToNumberType(overallType), Opcodes.IDIV);
					case MODULO -> getOpcode(restrictToNumberType(overallType), Opcodes.IREM);
				};

				replacementInsns.add(new InsnNode(opcode));
			}

			@Override
			public void visitCastExpression(CastExpression castExpression) {
				castExpression.operand.accept(this);

				DataType operandType = getExpressionType(groupInfo, castExpression.operand);
				addCastInsns(replacementInsns, operandType, castExpression.castType);
			}

			@Override
			public void visitFieldExpression(FieldExpression fieldExpression) {
				String fieldDesc = Utils.getDescriptor(fieldExpression.fieldType == null ? groupInfo.dataType : fieldExpression.fieldType);
				if (fieldExpression.isStatic) {
					String fieldOwner = fieldExpression.className.replace('.', '/');
					replacementInsns.add(new FieldInsnNode(Opcodes.GETSTATIC, fieldOwner, fieldExpression.fieldName, fieldDesc));
				} else {
					replacementInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
					String thisType = context.getContainingClass().name;
					for (FieldNode outerThisField : thisReferenceChains.get(fieldExpression)) {
						replacementInsns.add(new FieldInsnNode(Opcodes.GETFIELD, thisType, outerThisField.name, outerThisField.desc));
						thisType = Type.getType(outerThisField.desc).getInternalName();
					}
					replacementInsns.add(new FieldInsnNode(Opcodes.GETFIELD, thisType, fieldExpression.fieldName, fieldDesc));
				}
			}

			@Override
			public void visitLiteralExpression(LiteralExpression literalExpression) {
				replacementInsns.add(InstructionFactory.pushValue(literalToObject(literalExpression.literal)));
			}

			@Override
			public void visitUnaryExpression(UnaryExpression unaryExpression) {
				if (unaryExpression.operator == UnaryExpression.Operator.NEGATE && unaryExpression.operand instanceof LiteralExpression literalExpr) {
					Literal literal = literalExpr.literal;
					switch (literal) {
						case Literal.Integer(int value, int ignored) -> replacementInsns.add(InstructionFactory.pushInt(-value));
						case Literal.Long(long value, int ignored) -> replacementInsns.add(InstructionFactory.pushLong(-value));
						case Literal.Float(float value) -> replacementInsns.add(InstructionFactory.pushFloat(-value));
						case Literal.Double(double value) -> replacementInsns.add(InstructionFactory.pushDouble(-value));
						default -> {
						}
					}
				}

				unaryExpression.operand.accept(this);

				DataType operandType = getExpressionType(groupInfo, unaryExpression.operand);
				//noinspection ConstantValue
				Object ignored = switch (unaryExpression.operator) {
					case NEGATE -> {
						replacementInsns.add(new InsnNode(getOpcode(restrictToNumberType(operandType), Opcodes.INEG)));
						yield null;
					}
					case BIT_NOT -> {
						if (restrictToIntegralType(operandType) == DataType.INT) {
							replacementInsns.add(InstructionFactory.pushInt(0xffff_ffff));
							replacementInsns.add(new InsnNode(Opcodes.IXOR));
						} else {
							replacementInsns.add(InstructionFactory.pushLong(0xffff_ffff_ffff_ffffL));
							replacementInsns.add(new InsnNode(Opcodes.LXOR));
						}
						yield null;
					}
				};
			}

			private void buildStringConcatenation(IReplacementGenerator.IContext context, BinaryExpression binaryExpr) {
				List<Expression> stuffToConcatenate = new ArrayList<>();
				while (true) {
					stuffToConcatenate.addFirst(binaryExpr.rhs);
					if (!(binaryExpr.lhs instanceof BinaryExpression)) {
						stuffToConcatenate.addFirst(binaryExpr.lhs);
						break;
					}
					binaryExpr = (BinaryExpression) binaryExpr.lhs;
				}

				if (context.getContainingClass().version <= Opcodes.V1_8) {
					replacementInsns.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
					replacementInsns.add(new InsnNode(Opcodes.DUP));
					replacementInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
					for (Expression expression : stuffToConcatenate) {
						expression.accept(this);
						String sbDesc = "(" + Utils.getDescriptor(getExpressionType(groupInfo, expression)) + ")Ljava/lang/StringBuilder;";
						replacementInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", sbDesc, false));
					}
					replacementInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
				} else {
					StringBuilder recipe = new StringBuilder(stuffToConcatenate.size());
					StringBuilder concatType = new StringBuilder("(");
					for (Expression expression : stuffToConcatenate) {
						recipe.append('\1');
						concatType.append(Utils.getDescriptor(getExpressionType(groupInfo, expression)));
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

			private DataType restrictToIntegralType(DataType dataType) {
				if (dataType == DataType.BYTE || dataType == DataType.SHORT || dataType == DataType.CHAR || dataType == DataType.INT) {
					return DataType.INT;
				} else {
					return DataType.LONG;
				}
			}

			private DataType restrictToNumberType(DataType dataType) {
				return switch (dataType) {
					case BYTE, SHORT, CHAR, INT -> DataType.INT;
					case LONG -> DataType.LONG;
					case FLOAT -> DataType.FLOAT;
					default -> DataType.DOUBLE;
				};
			}
		});

		addCastInsns(replacementInsns, getExpressionType(groupInfo, replacement), literalType);

		context.getReplacementSet().addReplacement(targetInsn, replacementInsns);
	}

	private static Expression propagateExpectedTypeDown(GroupInfo groupInfo, Expression expression, DataType expectedType) {
		return expression.transform(new ExpressionTransformer() {
			private DataType myExpectedType = expectedType;

			@Override
			public Expression transformBinaryExpression(BinaryExpression binaryExpression) {
				if (myExpectedType == DataType.STRING) {
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
			public Expression transformCastExpression(CastExpression castExpression) {
				myExpectedType = castExpression.castType;
				return new CastExpression(castExpression.castType, castExpression.operand.transform(this));
			}

			@Override
			public Expression transformLiteralExpression(LiteralExpression literalExpression) {
				return new LiteralExpression(objectToLiteral(castLiteral(literalToObject(literalExpression.literal), myExpectedType, false)));
			}
		});
	}

	private static int getOpcode(DataType dataType, int intOpcode) {
		Type type = switch (dataType) {
			case BYTE, SHORT, CHAR, INT -> Type.INT_TYPE;
			case LONG -> Type.LONG_TYPE;
			case FLOAT -> Type.FLOAT_TYPE;
			case DOUBLE -> Type.DOUBLE_TYPE;
			case STRING -> Type.getObjectType("java/lang/String");
			case CLASS -> Type.getObjectType("java/lang/Class");
		};

		return type.getOpcode(intOpcode);
	}

	private static void addCastInsns(InsnList insns, DataType fromType, DataType toType) {
		Integer opcode = switch (fromType) {
			case BYTE, SHORT, CHAR, INT -> switch (toType) {
				case BYTE -> {
					if (fromType != DataType.BYTE) {
						insns.add(new InsnNode(Opcodes.I2B));
					}
					yield null;
				}
				case SHORT -> {
					if (fromType != DataType.BYTE && fromType != DataType.SHORT) {
						insns.add(new InsnNode(Opcodes.I2S));
					}
					yield null;
				}
				case CHAR -> {
					if (fromType != DataType.CHAR) {
						insns.add(new InsnNode(Opcodes.I2C));
					}
					yield null;
				}
				case LONG -> Opcodes.I2L;
				case FLOAT -> Opcodes.I2F;
				case DOUBLE -> Opcodes.I2D;
				default -> null;
			};
			case LONG -> switch (toType) {
				case BYTE, SHORT, CHAR, INT -> Opcodes.L2I;
				case FLOAT -> Opcodes.L2F;
				case DOUBLE -> Opcodes.L2D;
				default -> null;
			};
			case FLOAT -> switch (toType) {
				case BYTE, SHORT, CHAR, INT -> Opcodes.F2I;
				case LONG -> Opcodes.F2L;
				case DOUBLE -> Opcodes.F2D;
				default -> null;
			};
			case DOUBLE -> switch (toType) {
				case BYTE, SHORT, CHAR, INT -> Opcodes.D2I;
				case LONG -> Opcodes.D2L;
				case FLOAT -> Opcodes.D2F;
				default -> null;
			};
			default -> null;
		};
		if (opcode == null) {
			return;
		}

		insns.add(new InsnNode(opcode));
		switch (toType) {
			case BYTE -> insns.add(new InsnNode(Opcodes.I2B));
			case SHORT -> insns.add(new InsnNode(Opcodes.I2S));
			case CHAR -> insns.add(new InsnNode(Opcodes.I2C));
		}
	}

	private static AbstractInsnNode findStartOfNullCheck(AbstractInsnNode insn, int classVersion) {
		if (insn.getOpcode() != Opcodes.POP) {
			return null;
		}

		insn = AbstractInsnNodes.previousInstruction(insn);

		if (insn == null || insn.getType() != AbstractInsnNode.METHOD_INSN) {
			return null;
		}

		MethodInsnNode methodInsn = (MethodInsnNode) insn;

		boolean isNullCheck;
		if (classVersion <= Opcodes.V1_8) {
			isNullCheck = methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
					&& "getClass".equals(methodInsn.name)
					&& "()Ljava/lang/Class;".equals(methodInsn.desc);
		} else {
			isNullCheck = methodInsn.getOpcode() == Opcodes.INVOKESTATIC
					&& "java/util/Objects".equals(methodInsn.owner)
					&& "requireNonNull".equals(methodInsn.name)
					&& "(Ljava/lang/Object;)Ljava/lang/Object;".equals(methodInsn.desc);
		}

		return isNullCheck ? insn : null;
	}

	private static DataType getBinaryExpressionType(DataType leftType, DataType rightType) {
		if (leftType == DataType.STRING || rightType == DataType.STRING) {
			return DataType.STRING;
		} else if (leftType == DataType.DOUBLE || rightType == DataType.DOUBLE) {
			return DataType.DOUBLE;
		} else if (leftType == DataType.FLOAT || rightType == DataType.FLOAT) {
			return DataType.FLOAT;
		} else if (leftType == DataType.LONG || rightType == DataType.LONG) {
			return DataType.LONG;
		} else {
			return DataType.INT;
		}
	}

	private static DataType getUnaryExpressionType(DataType operandType) {
		if (operandType == DataType.BYTE || operandType == DataType.SHORT || operandType == DataType.CHAR) {
			return DataType.INT;
		} else {
			return operandType;
		}
	}

	static DataType getExpressionType(GroupInfo groupInfo, Expression expression) {
		DataType[] result = {null};
		expression.accept(new ExpressionVisitor() {
			@Override
			public void visitBinaryExpression(BinaryExpression binaryExpression) {
				binaryExpression.lhs.accept(this);
				DataType leftSide = result[0];
				binaryExpression.rhs.accept(this);
				DataType rightSide = result[0];
				result[0] = getBinaryExpressionType(leftSide, rightSide);
			}

			@Override
			public void visitCastExpression(CastExpression castExpression) {
				result[0] = castExpression.castType;
			}

			@Override
			public void visitFieldExpression(FieldExpression fieldExpression) {
				result[0] = fieldExpression.fieldType;
				if (result[0] == null) {
					result[0] = groupInfo.dataType;
				}
			}

			@Override
			public void visitLiteralExpression(LiteralExpression literalExpression) {
				Literal literal = literalExpression.literal;
				result[0] = switch (literal) {
					case Literal.Integer ignored -> DataType.INT;
					case Literal.Long ignored -> DataType.LONG;
					case Literal.Float ignored -> DataType.FLOAT;
					case Literal.Double ignored -> DataType.DOUBLE;
					case Literal.String ignored -> DataType.STRING;
					case Literal.Character ignored -> DataType.CHAR;
				};
			}

			@Override
			public void visitUnaryExpression(UnaryExpression unaryExpression) {
				super.visitUnaryExpression(unaryExpression);
				result[0] = getUnaryExpressionType(result[0]);
			}
		});

		return result[0];
	}

	@Nullable
	@Contract("_, _, false -> !null")
	public static Object castLiteral(Object literal, DataType destType, boolean requireExact) {
		return switch (destType) {
			case BYTE, SHORT, CHAR, INT -> {
				int result = ((Number) literal).intValue();
				boolean exactCast = result == ((Number) literal).doubleValue();
				yield exactCast || !requireExact ? result : null;
			}
			case LONG -> {
				long result = ((Number) literal).longValue();
				boolean exactCast = !Utils.isFloatingPoint(literal) || result == ((Number) literal).doubleValue();
				yield exactCast || !requireExact ? result : null;
			}
			case FLOAT -> {
				float result = ((Number) literal).floatValue();
				boolean exactCast = Utils.isFloatingPoint(literal) ? Double.compare(result, ((Number) literal).doubleValue()) == 0 : (long) result == ((Number) literal).longValue();
				yield exactCast || !requireExact ? result : null;
			}
			case DOUBLE -> {
				double result = ((Number) literal).doubleValue();
				boolean exactCast = Utils.isFloatingPoint(literal) || (long) result == ((Number) literal).longValue();
				yield exactCast || !requireExact ? result : null;
			}
			case STRING, CLASS -> literal;
		};
	}

	private static Object literalToObject(Literal literal) {
		return switch (literal) {
			case Literal.Integer(int value, int ignored) -> value;
			case Literal.Long(long value, int ignored) -> value;
			case Literal.Float(float value) -> value;
			case Literal.Double(double value) -> value;
			case Literal.Character(char value) -> value;
			case Literal.String(String value) -> value;
		};
	}

	private static Literal objectToLiteral(Object object) {
		return switch (object) {
			case Integer i -> new Literal.Integer(i);
			case Long l -> new Literal.Long(l);
			case Float f -> new Literal.Float(f);
			case Double d -> new Literal.Double(d);
			case Character c -> new Literal.Character(c);
			case String s -> new Literal.String(s);
			default -> throw new AssertionError("Unexpected literal type: " + object.getClass().getName());
		};
	}

	@Nullable
	private static OuterClassReference getOuterClassReference(IReplacementGenerator.IContext context, ClassNode innerClass) {
		if (innerClass.fields == null || innerClass.fields.isEmpty()) {
			return null;
		}

		int slashIndex = innerClass.name.lastIndexOf('/');
		int dollarIndex = innerClass.name.lastIndexOf('$');
		if (dollarIndex <= slashIndex) {
			return null;
		}

		String outerClassName = innerClass.name.substring(0, dollarIndex);

		if (!innerClass.fields.getFirst().desc.equals("L" + outerClassName + ";")) {
			return null;
		}

		ClassReader reader = context.getClassResolver().resolveClass(outerClassName);
		if (reader == null) {
			return null;
		}

		ClassNode outerClass = new ClassNode();
		reader.accept(outerClass, ClassReader.SKIP_CODE);
		boolean isInnerClassStatic = true;
		if (outerClass.innerClasses != null) {
			for (InnerClassNode innerClassNode : outerClass.innerClasses) {
				if (innerClassNode.name.equals(innerClass.name)) {
					isInnerClassStatic = (innerClassNode.access & Opcodes.ACC_STATIC) != 0;
					break;
				}
			}
		}

		if (isInnerClassStatic) {
			return null;
		}

		return new OuterClassReference(outerClass, innerClass.fields.getFirst());
	}

	@Nullable
	private static List<FieldNode> getThisReferenceChain(IReplacementGenerator.IContext context, FieldExpression fieldExpr) {
		String fieldOwner = fieldExpr.className.replace('.', '/');
		ClassNode clazz = context.getContainingClass();
		List<FieldNode> thisReferenceChain = new ArrayList<>();
		while (true) {
			if (context.getInheritanceChecker().isAssignableFrom(fieldOwner, clazz.name)) {
				return thisReferenceChain;
			}

			OuterClassReference outerClassRef = getOuterClassReference(context, clazz);
			if (outerClassRef == null) {
				return null;
			}
			thisReferenceChain.add(outerClassRef.outerThisReference);
			clazz = outerClassRef.outerClass;
		}
	}

	private record OuterClassReference(ClassNode outerClass, FieldNode outerThisReference) {
	}
}

package daomephsta.unpick.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.api.constantgroupers.ConstantGroup;
import daomephsta.unpick.api.constantgroupers.IConstantGrouper;
import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.impl.AbstractInsnNodes;
import daomephsta.unpick.impl.UnpickInterpreter;
import daomephsta.unpick.impl.UnpickValue;
import daomephsta.unpick.impl.representations.ReplacementInstructionGenerator.Context;
import daomephsta.unpick.impl.representations.ReplacementSet;

/**
 * Uninlines inlined values.
 * @author Daomephsta
 */
public final class ConstantUninliner {
	private final Logger logger;
	private final IConstantGrouper grouper;
	private final IClassResolver classResolver;
	private final IConstantResolver constantResolver;
	private final IInheritanceChecker inheritanceChecker;

	private ConstantUninliner(Logger logger, IConstantGrouper grouper, IClassResolver classResolver, IConstantResolver constantResolver, IInheritanceChecker inheritanceChecker) {
		this.grouper = grouper;
		this.classResolver = classResolver;
		this.constantResolver = constantResolver;
		this.inheritanceChecker = inheritanceChecker;
		this.logger = logger;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Uninlines all inlined values in the specified class.
	 * @param classNode the class to transform, as a ClassNode.
	 */
	public void transform(ClassNode classNode) {
		Map<String, MethodNode> methods = new HashMap<>();
		Map<String, Frame<UnpickValue>[]> frames = new HashMap<>();

		for (MethodNode method : classNode.methods) {
			String methodKey = getMethodKey(method);
			methods.put(methodKey, method);
			frames.put(methodKey, analyzeMethod(classNode, method));
		}

		Map<String, List<LambdaUsage>> lambdaUsages = indexLambdaUsages(classNode);

		List<ReplacementSet> replacements = new ArrayList<>();

		for (MethodNode method : classNode.methods) {
			ReplacementSet replacementsForMethod = transformMethod(
					classNode,
					method,
					new MethodTransformContext(
							methods,
							frames,
							lambdaUsages,
							new HashSet<>()
					)
			);
			if (replacementsForMethod != null) {
				replacements.add(replacementsForMethod);
			}
		}

		replacements.forEach(ReplacementSet::apply);
	}

	/**
	 * Uninlines all values in a specific method. Note that this doesn't do any multi-method analysis, such as for
	 * lambdas, so {@link #transform} is preferred wherever possible.
	 *
	 * @param methodOwner the owner class of this method.
	 * @param method the method to transform.
	 */
	public void transformMethod(ClassNode methodOwner, MethodNode method) {
		Frame<UnpickValue>[] frames = analyzeMethod(methodOwner, method);
		if (frames != null) {
			ReplacementSet replacements = transformMethod(
					methodOwner,
					method,
					new MethodTransformContext(
							Map.of(getMethodKey(method), method),
							Map.of(getMethodKey(method), frames),
							Map.of(),
							new HashSet<>()
					)
			);
			if (replacements != null) {
				replacements.apply();
			} else {
				logger.log(Level.WARNING, () -> "No replacements for analyzed method " + getMethodKey(method) + "?!");
			}
		}
	}

	@Nullable
	private ReplacementSet transformMethod(ClassNode methodOwner, MethodNode method, MethodTransformContext transformContext) {
		try {
			Frame<UnpickValue>[] frames = transformContext.frames.get(getMethodKey(method));
			if (frames == null) {
				return null;
			}

			logger.log(Level.FINEST, () -> String.format("Transforming method %s.%s%s", methodOwner.name, method.name, method.desc));
			ReplacementSet replacementSet = new ReplacementSet(method.instructions);

			Map<AbstractInsnNode, ConstantGroup> groups = new HashMap<>();
			Set<AbstractInsnNode> ungrouped = new HashSet<>();

			for (int index = 0; index < method.instructions.size(); index++) {
				AbstractInsnNode insn = method.instructions.get(index);
				if (AbstractInsnNodes.hasLiteralValue(insn) && !ungrouped.contains(insn)) {
					Frame<UnpickValue> frame = index + 1 >= frames.length ? null : frames[index + 1];
					if (frame != null) {
						UnpickValue unpickValue = frame.getStack(frame.getStackSize() - 1);
						ConstantGroup group = groups.get(insn);
						if (group == null) {
							group = findGroup(methodOwner.name, method, unpickValue, transformContext);

							if (group == null) {
								group = grouper.getDefaultGroup();
							}

							if (group == null) {
								ungrouped.addAll(unpickValue.getUsages());
							} else {
								for (AbstractInsnNode usage : unpickValue.getUsages()) {
									groups.put(usage, group);
								}
							}
						}

						if (group != null && !isAssigningToConstant(insn)) {
							Context context = new Context(classResolver, constantResolver, inheritanceChecker, replacementSet, methodOwner, method, insn, frames, logger);
							group.apply(context);
						}
					}
				}
			}

			return replacementSet;
		} catch (Throwable e) {
			logger.log(Level.WARNING, String.format("Failed to transform method %s.%s%s", methodOwner.name, method.name, method.desc), e);
			return null;
		}
	}

	@Nullable
	private Frame<UnpickValue>[] analyzeMethod(ClassNode methodOwner, MethodNode method) {
		logger.log(Level.FINEST, () -> String.format("Running dataflow on %s.%s%s", methodOwner.name, method.name, method.desc));
		try {
			return new Analyzer<>(new UnpickInterpreter(method, inheritanceChecker)).analyze(methodOwner.name, method);
		} catch (Throwable e) {
			logger.log(Level.WARNING, String.format("Dataflow on %s.%s%s failed", methodOwner.name, method.name, method.desc), e);
			return null;
		}
	}

	@Nullable
	private ConstantGroup findGroup(String methodOwner, MethodNode method, UnpickValue unpickValue, MethodTransformContext context) {
		if (!context.checkedMethods.add(getMethodKey(method))) {
			// protect against infinite recursion
			return null;
		}

		try {
			ConstantGroup group = null;

			for (int parameterSource : unpickValue.getParameterSources()) {
				ConstantGroup g = processParameterSource(methodOwner, method, parameterSource, context);
				if (g != null) {
					if (group != null && !g.getName().equals(group.getName())) {
						warnGroupConflict(g, group, methodOwner, method);
						return null;
					}
					group = g;
				}
			}

			for (IReplacementGenerator.IParameterUsage paramUsage : unpickValue.getParameterUsages()) {
				ConstantGroup g = processParameterUsage(methodOwner, paramUsage, context);
				if (g != null) {
					if (group != null && !g.getName().equals(group.getName())) {
						warnGroupConflict(g, group, methodOwner, method);
						return null;
					}
					group = g;
				}
			}

			for (AbstractInsnNode usage : unpickValue.getUsages()) {
				ConstantGroup g = processUsage(methodOwner, method, usage, context);
				if (g != null) {
					if (group != null && !g.getName().equals(group.getName())) {
						warnGroupConflict(g, group, methodOwner, method);
						return null;
					}
					group = g;
				}
			}

			return group;
		} finally {
			context.checkedMethods.remove(getMethodKey(method));
		}
	}

	@Nullable
	private ConstantGroup processParameterSource(String methodOwner, MethodNode method, int parameterSource, MethodTransformContext context) {
		ConstantGroup group = grouper.getMethodParameterGroup(methodOwner, method.name, method.desc, parameterSource);
		if (group != null) {
			return group;
		}

		List<LambdaUsage> lambdaUsagesForMethod = context.lambdaUsages.get(getMethodKey(method));
		if (lambdaUsagesForMethod != null) {
			for (LambdaUsage lambdaUsage : lambdaUsagesForMethod) {
				Frame<UnpickValue>[] containingMethodFrames = context.frames.get(getMethodKey(lambdaUsage.method));
				if (containingMethodFrames == null) {
					continue;
				}

				Frame<UnpickValue> frame = containingMethodFrames[lambdaUsage.method.instructions.indexOf(lambdaUsage.indy)];
				if (frame == null) {
					continue;
				}

				int numCaptures = Type.getArgumentCount(lambdaUsage.indy.desc);
				if (!isStaticLambdaInvocation(lambdaUsage.indy)) {
					// don't count "this" as a capture for non-static lambdas, even though it is one in the bytecode
					numCaptures--;
				}

				if (parameterSource < numCaptures) {
					// Parameter is a lambda capture
					UnpickValue lambdaCapture = frame.getStack(frame.getStackSize() - numCaptures + parameterSource);
					if (lambdaCapture != null) {
						ConstantGroup g = findGroup(methodOwner, lambdaUsage.method, lambdaCapture, context);
						if (g != null) {
							if (group != null && !g.getName().equals(group.getName())) {
								warnGroupConflict(g, group, methodOwner, method);
								return null;
							}
							group = g;
						}
					}
				} else {
					// Parameter is an inherent parameter of the functional interface
					String samOwner = Type.getReturnType(lambdaUsage.indy.desc).getInternalName();
					String samName = lambdaUsage.indy.name;
					String samDesc = ((Type) lambdaUsage.indy.bsmArgs[0]).getDescriptor();
					ConstantGroup g = grouper.getMethodParameterGroup(samOwner, samName, samDesc, parameterSource - numCaptures);
					if (g != null) {
						if (group != null && !g.getName().equals(group.getName())) {
							warnGroupConflict(g, group, methodOwner, method);
							return null;
						}
						group = g;
					}
				}
			}
		}

		return group;
	}

	@Nullable
	private ConstantGroup processParameterUsage(String methodOwner, IReplacementGenerator.IParameterUsage paramUsage, MethodTransformContext context) {
		if (paramUsage.getMethodInvocation() instanceof InvokeDynamicInsnNode indy) {
			if ("java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner())) {
				Handle lambdaMethod = (Handle) indy.bsmArgs[1];
				boolean staticLambdaInvocation = isStaticLambdaInvocation(indy);
				if (!staticLambdaInvocation && paramUsage.getParamIndex() == 0) {
					// Lambda parameter is the instance parameter, which is possible through e.g. method references or
					// other types assigned to the ACONST_NULL instruction which gets tracked. Since unpick has no way
					// to target the instance parameter we will ignore it in this case.
					return null;
				}
				int paramIndex = staticLambdaInvocation ? paramUsage.getParamIndex() : paramUsage.getParamIndex() - 1;
				ConstantGroup group = grouper.getMethodParameterGroup(lambdaMethod.getOwner(), lambdaMethod.getName(), lambdaMethod.getDesc(), paramIndex);
				if (group != null) {
					return group;
				}
				if (!lambdaMethod.getOwner().equals(methodOwner)) {
					return null;
				}

				String lambdaKey = getMethodKey(lambdaMethod);
				Frame<UnpickValue>[] lambdaFrames = context.frames.get(lambdaKey);
				if (lambdaFrames == null || lambdaFrames.length == 0) {
					return null;
				}

				Frame<UnpickValue> firstLambdaFrame = lambdaFrames[0];
				if (firstLambdaFrame == null) {
					return null;
				}

				int localIndex = lambdaMethod.getTag() == Opcodes.H_INVOKESTATIC ? 0 : 1;
				Type[] lambdaArgs = Type.getArgumentTypes(lambdaMethod.getDesc());
				for (int i = 0; i < lambdaArgs.length; i++) {
					if (i == paramIndex) {
						break;
					}
					localIndex += lambdaArgs[i].getSize();
				}
				UnpickValue lambdaParam = firstLambdaFrame.getLocal(localIndex);
				if (lambdaParam == null) {
					return null;
				}

				return findGroup(methodOwner, context.methods.get(lambdaKey), lambdaParam, context);
			}

			return null;
		} else {
			MethodInsnNode methodInsn = (MethodInsnNode) paramUsage.getMethodInvocation();
			return grouper.getMethodParameterGroup(methodInsn.owner, methodInsn.name, methodInsn.desc, paramUsage.getParamIndex());
		}
	}

	@Nullable
	private ConstantGroup processUsage(String methodOwner, MethodNode enclosingMethod, AbstractInsnNode usage, MethodTransformContext context) {
		if (usage.getType() == AbstractInsnNode.FIELD_INSN) {
			FieldInsnNode fieldInsn = (FieldInsnNode) usage;
			return grouper.getFieldGroup(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
		}

		if (usage.getType() == AbstractInsnNode.METHOD_INSN) {
			// A method "usage" is from the return type of a method invocation
			MethodInsnNode method = (MethodInsnNode) usage;
			return grouper.getMethodReturnGroup(method.owner, method.name, method.desc);
		}

		if (usage.getOpcode() >= Opcodes.IRETURN && usage.getOpcode() <= Opcodes.RETURN) {
			ConstantGroup group = grouper.getMethodReturnGroup(methodOwner, enclosingMethod.name, enclosingMethod.desc);
			if (group != null) {
				return group;
			}

			List<LambdaUsage> lambdaUsages = context.lambdaUsages.get(getMethodKey(enclosingMethod));
			if (lambdaUsages != null) {
				for (LambdaUsage lambdaUsage : lambdaUsages) {
					String samOwner = Type.getReturnType(lambdaUsage.indy.desc).getInternalName();
					String samName = lambdaUsage.indy.name;
					String samDesc = ((Type) lambdaUsage.indy.bsmArgs[0]).getDescriptor();
					ConstantGroup g = grouper.getMethodReturnGroup(samOwner, samName, samDesc);
					if (g != null) {
						if (group != null && !g.getName().equals(group.getName())) {
							warnGroupConflict(g, group, methodOwner, enclosingMethod);
							return null;
						}
						group = g;
					}
				}

				return group;
			}
		}

		return null;
	}

	private boolean isAssigningToConstant(AbstractInsnNode insn) {
		AbstractInsnNode nextInsn = AbstractInsnNodes.nextInstruction(insn);
		if (nextInsn == null) {
			return false;
		}
		if (nextInsn.getOpcode() != Opcodes.PUTFIELD && nextInsn.getOpcode() != Opcodes.PUTSTATIC) {
			return false;
		}

		FieldInsnNode fieldInsn = (FieldInsnNode) nextInsn;
		// is our field a constant?
		IConstantResolver.ResolvedConstant resolvedConstant = constantResolver.resolveConstant(fieldInsn.owner, fieldInsn.name);
		return resolvedConstant != null && fieldInsn.desc.equals(resolvedConstant.type().getDescriptor());
	}

	private Map<String, List<LambdaUsage>> indexLambdaUsages(ClassNode classNode) {
		try {
			Set<String> syntheticMethods = new HashSet<>();
			for (MethodNode method : classNode.methods) {
				if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
					syntheticMethods.add(getMethodKey(method));
				}
			}

			Map<String, List<LambdaUsage>> lambdaUsages = new HashMap<>();
			for (MethodNode method : classNode.methods) {
				for (AbstractInsnNode insn : method.instructions) {
					if (insn instanceof InvokeDynamicInsnNode indy && "java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner())) {
						Handle lambdaMethod = (Handle) indy.bsmArgs[1];
						String lambdaKey = getMethodKey(lambdaMethod);
						if (lambdaMethod.getOwner().equals(classNode.name) && syntheticMethods.contains(lambdaKey)) {
							lambdaUsages.computeIfAbsent(lambdaKey, k -> new ArrayList<>(1))
									.add(new LambdaUsage(method, indy));
						}
					}
				}
			}

			return lambdaUsages;
		} catch (Throwable e) {
			logger.log(Level.WARNING, "Error processing lambda usages for class " + classNode.name, e);
			return Map.of();
		}
	}

	private static boolean isStaticLambdaInvocation(InvokeDynamicInsnNode insn) {
		int kind = ((Handle) insn.bsmArgs[1]).getTag();
		return kind == Opcodes.H_GETSTATIC || kind == Opcodes.H_PUTSTATIC || kind == Opcodes.H_INVOKESTATIC || kind == Opcodes.H_NEWINVOKESPECIAL;
	}

	private static String getMethodKey(MethodNode method) {
		return method.name + method.desc;
	}

	private static String getMethodKey(Handle handle) {
		return handle.getName() + handle.getDesc();
	}

	private void warnGroupConflict(ConstantGroup group1, ConstantGroup group2, String methodOwner, MethodNode enclosingMethod) {
		logger.log(Level.WARNING, () -> String.format("Conflicting groups %s and %s competing for the same constant in method %s.%s%s", group1.getName(), group2.getName(), methodOwner, enclosingMethod.name, enclosingMethod.desc));
	}

	private record MethodTransformContext(
			Map<String, MethodNode> methods,
			Map<String, Frame<UnpickValue>[]> frames,
			Map<String, List<LambdaUsage>> lambdaUsages,
			Set<String> checkedMethods
	) {
	}

	private record LambdaUsage(MethodNode method, InvokeDynamicInsnNode indy) {
	}

	public static final class Builder {
		@Nullable
		private Logger logger;
		@Nullable
		private IConstantGrouper grouper;
		@Nullable
		private IClassResolver classResolver;
		@Nullable
		private IConstantResolver constantResolver;
		@Nullable
		private IInheritanceChecker inheritanceChecker;

		private Builder() {
		}

		public Builder logger(Logger logger) {
			this.logger = logger;
			return this;
		}

		public Builder grouper(IConstantGrouper grouper) {
			this.grouper = grouper;
			return this;
		}

		public Builder classResolver(IClassResolver classResolver) {
			this.classResolver = classResolver;
			return this;
		}

		public Builder constantResolver(IConstantResolver constantResolver) {
			this.constantResolver = constantResolver;
			return this;
		}

		public Builder inheritanceChecker(IInheritanceChecker inheritanceChecker) {
			this.inheritanceChecker = inheritanceChecker;
			return this;
		}

		public ConstantUninliner build() {
			Objects.requireNonNull(grouper, "Must add grouper to builder");
			Objects.requireNonNull(classResolver, "Must add classResolver to builder");

			if (logger == null) {
				logger = Logger.getLogger("unpick");
			}

			if (constantResolver == null) {
				constantResolver = classResolver.asConstantResolver();
			}

			if (inheritanceChecker == null) {
				inheritanceChecker = classResolver.asInheritanceChecker();
			}

			return new ConstantUninliner(logger, grouper, classResolver, constantResolver, inheritanceChecker);
		}
	}
}

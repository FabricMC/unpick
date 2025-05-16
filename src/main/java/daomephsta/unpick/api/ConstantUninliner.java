package daomephsta.unpick.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
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
	 * Unlines all inlined values in the specified class.
	 * @param classNode the class to transform, as a ClassNode.
	 */
	public void transform(ClassNode classNode) {
		for (MethodNode method : classNode.methods) {
			transformMethod(classNode, method);
		}
	}

	public void transformMethod(ClassNode methodOwner, MethodNode method) {
		logger.log(Level.INFO, String.format("Processing %s.%s%s", methodOwner.name, method.name, method.desc));
		try {
			ReplacementSet replacementSet = new ReplacementSet(method.instructions);
			Frame<UnpickValue>[] frames = new Analyzer<>(new UnpickInterpreter(method, inheritanceChecker)).analyze(methodOwner.name, method);

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
							group = findGroup(methodOwner.name, method, unpickValue);
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

			replacementSet.apply();
		} catch (AnalyzerException e) {
			logger.log(Level.WARNING, String.format("Processing %s.%s%s failed", methodOwner.name, method.name, method.desc), e);
		}
	}

	@Nullable
	private ConstantGroup findGroup(String methodOwner, MethodNode method, UnpickValue unpickValue) {
		ConstantGroup group = null;

		for (int parameterSource : unpickValue.getParameterSources()) {
			ConstantGroup g = grouper.getMethodParameterGroup(methodOwner, method.name, method.desc, parameterSource);
			if (g != null) {
				if (group != null && !g.getName().equals(group.getName())) {
					ConstantGroup group_f = group;
					logger.log(Level.WARNING, () -> String.format("Conflicting groups %s and %s competing for the same constant", g.getName(), group_f.getName()));
					return null;
				}
				group = g;
			}
		}

		for (IReplacementGenerator.IParameterUsage paramUsage : unpickValue.getParameterUsages()) {
			ConstantGroup g = processParameterUsage(paramUsage);
			if (g != null) {
				if (group != null && !g.getName().equals(group.getName())) {
					ConstantGroup group_f = group;
					logger.log(Level.WARNING, () -> String.format("Conflicting groups %s and %s competing for the same constant", g.getName(), group_f.getName()));
					return null;
				}
				group = g;
			}
		}

		for (AbstractInsnNode usage : unpickValue.getUsages()) {
			ConstantGroup g = processUsage(methodOwner, method, usage);
			if (g != null) {
				if (group != null && !g.getName().equals(group.getName())) {
					ConstantGroup group_f = group;
					logger.log(Level.WARNING, () -> String.format("Conflicting groups %s and %s competing for the same constant", g.getName(), group_f.getName()));
					return null;
				}
				group = g;
			}
		}

		if (group != null) {
			return group;
		}

		return grouper.getDefaultGroup();
	}

	@Nullable
	private ConstantGroup processParameterUsage(IReplacementGenerator.IParameterUsage paramUsage) {
		if (paramUsage.getMethodInvocation().getOpcode() == Opcodes.INVOKEDYNAMIC) {
			InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) paramUsage.getMethodInvocation();

			if ("java/lang/invoke/LambdaMetafactory".equals(invokeDynamicInsn.bsm.getOwner()) && "metafactory".equals(invokeDynamicInsn.bsm.getName())) {
				Handle lambdaMethod = (Handle) invokeDynamicInsn.bsmArgs[1];
				int kind = lambdaMethod.getTag();
				boolean hasThis = kind != Opcodes.H_GETSTATIC && kind != Opcodes.H_PUTSTATIC && kind != Opcodes.H_INVOKESTATIC && kind != Opcodes.H_NEWINVOKESPECIAL;
				int paramIndex = hasThis ? paramUsage.getParamIndex() - 1 : paramUsage.getParamIndex();
				return grouper.getMethodParameterGroup(lambdaMethod.getOwner(), lambdaMethod.getName(), lambdaMethod.getDesc(), paramIndex);
			}

			return null;
		} else {
			MethodInsnNode methodInsn = (MethodInsnNode) paramUsage.getMethodInvocation();
			return grouper.getMethodParameterGroup(methodInsn.owner, methodInsn.name, methodInsn.desc, paramUsage.getParamIndex());
		}
	}

	@Nullable
	private ConstantGroup processUsage(String methodOwner, MethodNode enclosingMethod, AbstractInsnNode usage) {
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
			return grouper.getMethodReturnGroup(methodOwner, enclosingMethod.name, enclosingMethod.desc);
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

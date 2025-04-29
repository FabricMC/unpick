package daomephsta.unpick.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.impl.AbstractInsnNodes;
import daomephsta.unpick.impl.UnpickInterpreter;
import daomephsta.unpick.impl.UnpickValue;
import daomephsta.unpick.impl.representations.ReplacementInstructionGenerator.Context;
import daomephsta.unpick.impl.representations.ReplacementSet;

/**
 * Uninlines inlined values.
 * @author Daomephsta
 */
public class ConstantUninliner {
	private final Logger logger;
	private final IConstantMapper mapper;
	private final IConstantResolver constantResolver;

	/**
	 * Constructs a new instance of ConstantUninliner that maps
	 * values to constants with {@code mapper}.
	 * @param mapper an instance of IConstantMapper.
	 * @param constantResolver an instance of IConstantResolver for resolving constant types and
	 * values.
	 */
	public ConstantUninliner(IConstantMapper mapper, IConstantResolver constantResolver) {
		this(mapper, constantResolver, Logger.getLogger("unpick"));
	}

	/**
	 * Constructs a new instance of ConstantUninliner that maps
	 * values to constants with {@code mapper}.
	 * @param mapper an instance of IConstantMapper.
	 * @param constantResolver an instance of IConstantResolver for resolving constant types and
	 * values.
	 * @param logger a logger for debug logging.
	 */
	public ConstantUninliner(IConstantMapper mapper, IConstantResolver constantResolver, Logger logger) {
		this.mapper = mapper;
		this.constantResolver = constantResolver;
		this.logger = logger;
	}

	/**
	 * Unlines all inlined values in the specified class.
	 * @param classNode the class to transform, as a ClassNode.
	 */
	public void transform(ClassNode classNode) {
		for (MethodNode method : classNode.methods) {
			transformMethod(classNode.name, method);
		}
	}

	/**
	 * Unlines all inlined values in the specified method.
	 * @param methodOwner the internal name of the class that owns {@code method}.
	 * @param method the class to transform, as a MethodNode.
	 */
	public void transformMethod(String methodOwner, MethodNode method) {
		logger.log(Level.INFO, String.format("Processing %s.%s%s", methodOwner, method.name, method.desc));
		try {
			ReplacementSet replacementSet = new ReplacementSet(method.instructions);
			Frame<UnpickValue>[] frames = new Analyzer<>(new UnpickInterpreter(method)).analyze(methodOwner, method);

			Map<AbstractInsnNode, Consumer<Context>> mappers = new HashMap<>();
			Set<AbstractInsnNode> unmapped = new HashSet<>();

			for (int index = 0; index < method.instructions.size(); index++) {
				AbstractInsnNode insn = method.instructions.get(index);
				if (AbstractInsnNodes.hasLiteralValue(insn) && !unmapped.contains(insn)) {
					Frame<UnpickValue> frame = index + 1 >= frames.length ? null : frames[index + 1];
					if (frame != null) {
						UnpickValue unpickValue = frame.getStack(frame.getStackSize() - 1);
						Consumer<Context> mapper = mappers.get(insn);
						if (mapper == null) {
							mapper = findMapper(methodOwner, method, unpickValue);
							if (mapper == null) {
								unmapped.addAll(unpickValue.getUsages());
							} else {
								for (AbstractInsnNode usage : unpickValue.getUsages()) {
									mappers.put(usage, mapper);
								}
							}
						}

						if (mapper != null) {
							Context context = new Context(constantResolver, replacementSet, insn, method.instructions, frames, logger);
							mapper.accept(context);
						}
					}
				}
			}

			replacementSet.apply();
		} catch (AnalyzerException e) {
			logger.log(Level.WARNING, String.format("Processing %s.%s%s failed", methodOwner, method.name, method.desc), e);
		}
	}

	private Consumer<Context> findMapper(String methodOwner, MethodNode method, UnpickValue unpickValue) {
		for (int parameterSource : unpickValue.getParameterSources()) {
			Consumer<Context> ret = processParameterSource(methodOwner, method, parameterSource);
			if (ret != null) {
				return ret;
			}
		}
		for (UnpickValue.MethodUsage methodUsage : unpickValue.getMethodUsages()) {
			Consumer<Context> ret = processMethodUsage(methodUsage);
			if (ret != null) {
				return ret;
			}
		}
		for (AbstractInsnNode usage : unpickValue.getUsages()) {
			Consumer<Context> ret = processUsage(methodOwner, method, usage);
			if (ret != null) {
				return ret;
			}
		}

		return null;
	}

	private Consumer<Context> processParameterSource(String methodOwner, MethodNode enclosingMethod, int parameterIndex) {
		if (!mapper.targets(methodOwner, enclosingMethod.name, enclosingMethod.desc)) {
			return null;
		}
		if (!mapper.targetsParameter(methodOwner, enclosingMethod.name, enclosingMethod.desc, parameterIndex)) {
			return null;
		}
		logger.log(Level.INFO, String.format("Using enclosing method %s.%s%s parameter %d", methodOwner, enclosingMethod.name, enclosingMethod.desc, parameterIndex));
		return context -> mapper.mapParameter(methodOwner, enclosingMethod.name, enclosingMethod.desc, parameterIndex, context);
	}

	private Consumer<Context> processMethodUsage(UnpickValue.MethodUsage methodUsage) {
		if (methodUsage.getMethodInvocation().getOpcode() == Opcodes.INVOKEDYNAMIC) {
			InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) methodUsage.getMethodInvocation();

			if ("java/lang/invoke/LambdaMetafactory".equals(invokeDynamicInsn.bsm.getOwner()) && "metafactory".equals(invokeDynamicInsn.bsm.getName())) {
				Handle lambdaMethod = (Handle) invokeDynamicInsn.bsmArgs[1];
				if (!mapper.targets(lambdaMethod.getOwner(), lambdaMethod.getName(), lambdaMethod.getDesc())) {
					return null;
				}
				int kind = lambdaMethod.getTag();
				boolean hasThis = kind != Opcodes.H_GETSTATIC && kind != Opcodes.H_PUTSTATIC && kind != Opcodes.H_INVOKESTATIC && kind != Opcodes.H_NEWINVOKESPECIAL;
				int paramIndex = hasThis ? methodUsage.getParamIndex() - 1 : methodUsage.getParamIndex();
				if (!mapper.targetsParameter(lambdaMethod.getOwner(), lambdaMethod.getName(), lambdaMethod.getDesc(), paramIndex)) {
					return null;
				}
				logger.log(Level.INFO, String.format("Using lambda %s.%s%s captured parameter %d", lambdaMethod.getOwner(), lambdaMethod.getName(), lambdaMethod.getDesc(), paramIndex));
				return context -> mapper.mapParameter(lambdaMethod.getOwner(), lambdaMethod.getName(), lambdaMethod.getDesc(), paramIndex, context);
			}

			return null;
		} else {
			MethodInsnNode methodInsn = (MethodInsnNode) methodUsage.getMethodInvocation();
			if (!mapper.targets(methodInsn.owner, methodInsn.name, methodInsn.desc)) {
				return null;
			}
			if (!mapper.targetsParameter(methodInsn.owner, methodInsn.name, methodInsn.desc, methodUsage.getParamIndex())) {
				return null;
			}
			logger.log(Level.INFO, String.format("Using method invocation %s.%s%s parameter %d", methodInsn.owner, methodInsn.name, methodInsn.desc, methodUsage.getParamIndex()));
			return context -> mapper.mapParameter(methodInsn.owner, methodInsn.name, methodInsn.desc, methodUsage.getParamIndex(), context);
		}
	}

	private Consumer<Context> processUsage(String methodOwner, MethodNode enclosingMethod, AbstractInsnNode usage) {
		if (usage.getType() == AbstractInsnNode.METHOD_INSN) {
			// A method "usage" is from the return type of a method invocation
			MethodInsnNode method = (MethodInsnNode) usage;
			if (!mapper.targets(method.owner, method.name, method.desc)) {
				return null;
			}
			if (!mapper.targetsReturn(method.owner, method.name, method.desc)) {
				return null;
			}
			logger.log(Level.INFO, String.format("Using method invocation %s.%s%s return type", method.owner, method.name, method.desc));
			return context -> mapper.mapReturn(method.owner, method.name, method.desc, context);
		}

		if (usage.getOpcode() >= Opcodes.IRETURN && usage.getOpcode() <= Opcodes.RETURN) {
			if (!mapper.targets(methodOwner, enclosingMethod.name, enclosingMethod.desc)) {
				return null;
			}
			if (!mapper.targetsReturn(methodOwner, enclosingMethod.name, enclosingMethod.desc)) {
				return null;
			}
			logger.log(Level.INFO, String.format("Using enclosing method %s.%s%s return type", methodOwner, enclosingMethod.name, enclosingMethod.desc));
			return context -> mapper.mapReturn(methodOwner, enclosingMethod.name, enclosingMethod.desc, context);
		}

		return null;
	}
}

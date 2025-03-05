package daomephsta.unpick.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import daomephsta.unpick.api.constantgroupers.ConstantGroup;
import daomephsta.unpick.api.constantgroupers.IConstantGrouper;
import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.api.inheritancecheckers.IInheritanceChecker;
import daomephsta.unpick.impl.AbstractInsnNodes;
import daomephsta.unpick.impl.UnpickInterpreter;
import daomephsta.unpick.impl.UnpickValue;
import daomephsta.unpick.impl.inheritancecheckers.ClasspathInheritanceChecker;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.impl.representations.ReplacementInstructionGenerator.Context;
import daomephsta.unpick.impl.representations.ReplacementSet;

/**
 * Uninlines inlined values 
 * @author Daomephsta
 */
public class ConstantUninliner
{
	private final Logger logger;
	private final IConstantGrouper grouper;
	private final IClassResolver classResolver;
	private final IConstantResolver constantResolver;
	private final IInheritanceChecker inheritanceChecker;

	/**
	 * Constructs a new instance of ConstantUninliner that maps
	 * values to constants with {@code mapper}.
	 * @param mapper an instance of IConstantMapper.
	 * @param constantResolver an instance of IConstantResolver for resolving constant types and 
	 * values.
	 *
	 * @deprecated Use {@link #ConstantUninliner(IConstantGrouper, IClassResolver, IConstantResolver, IInheritanceChecker)} instead.
	 */
	@Deprecated
	public ConstantUninliner(IConstantMapper mapper, IConstantResolver constantResolver)
	{
		this(mapper, constantResolver, Logger.getLogger("unpick"));
	}
	
	/**
	 * Constructs a new instance of ConstantUninliner that maps
	 * values to constants with {@code mapper}.
	 * @param mapper an instance of IConstantMapper.
	 * @param constantResolver an instance of IConstantResolver for resolving constant types and 
	 * values.
	 * @param logger a logger for debug logging.
	 *
	 * @deprecated Use {@link #ConstantUninliner(IConstantGrouper, IClassResolver, IConstantResolver, IInheritanceChecker, Logger)} instead.
	 */
	@Deprecated
	public ConstantUninliner(IConstantMapper mapper, IConstantResolver constantResolver, Logger logger)
	{
		this(mapper, internalName ->
		{
			try
			{
				return new ClassReader(internalName);
			}
			catch (IOException e)
			{
				throw new IClassResolver.ClassResolutionException(e);
			}
		}, constantResolver, new ClasspathInheritanceChecker(), logger);
	}

	public ConstantUninliner(IConstantGrouper grouper, IClassResolver classResolver, IConstantResolver constantResolver, IInheritanceChecker inheritanceChecker)
	{
		this(grouper, classResolver, constantResolver, inheritanceChecker, Logger.getLogger("unpick"));
	}

	public ConstantUninliner(IConstantGrouper grouper, IClassResolver classResolver, IConstantResolver constantResolver, IInheritanceChecker inheritanceChecker, Logger logger)
	{
		this.grouper = grouper;
		this.classResolver = classResolver;
		this.constantResolver = constantResolver;
		this.inheritanceChecker = inheritanceChecker;
		this.logger = logger;
	}

	/**
	 * Unlines all inlined values in the specified class.
	 * @param classNode the class to transform, as a ClassNode.
	 */
	public void transform(ClassNode classNode)
	{
		for (MethodNode method : classNode.methods)
		{
			transformMethod(classNode, method);
		}
	}

	/**
	 * Unlines all inlined values in the specified method.
	 * @param methodOwner the internal name of the class that owns {@code method}.
	 * @param method the class to transform, as a MethodNode.
	 *
	 * @deprecated Use {@link #transformMethod(ClassNode, MethodNode)} instead.
	 */
	@Deprecated
	public void transformMethod(String methodOwner, MethodNode method)
	{
		ClassNode fakeClassNode = new ClassNode();
		fakeClassNode.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, methodOwner, null, "java/lang/Object", null);
		method.accept(fakeClassNode);
		fakeClassNode.visitEnd();
		transformMethod(fakeClassNode, method);
	}

	public void transformMethod(ClassNode methodOwner, MethodNode method)
	{
		logger.log(Level.INFO, String.format("Processing %s.%s%s", methodOwner.name, method.name, method.desc));
		try
		{ 
			ReplacementSet replacementSet = new ReplacementSet(method.instructions);
			Frame<UnpickValue>[] frames = new Analyzer<>(new UnpickInterpreter(method, inheritanceChecker)).analyze(methodOwner.name, method);

			Map<AbstractInsnNode, ConstantGroup> groups = new HashMap<>();
			Set<AbstractInsnNode> ungrouped = new HashSet<>();

			for (int index = 0; index < method.instructions.size(); index++)
			{
				AbstractInsnNode insn = method.instructions.get(index);
				if (AbstractInsnNodes.hasLiteralValue(insn) && !ungrouped.contains(insn))
				{
					Frame<UnpickValue> frame = index + 1 >= frames.length ? null : frames[index + 1];
					if (frame != null)
					{
						UnpickValue unpickValue = frame.getStack(frame.getStackSize() - 1);
						ConstantGroup group = groups.get(insn);
						if (group == null)
						{
							group = findGroup(methodOwner.name, method, unpickValue);
							if (group == null)
								ungrouped.addAll(unpickValue.getUsages());
							else
							{
								for (AbstractInsnNode usage : unpickValue.getUsages())
								{
									groups.put(usage, group);
								}
							}
						}

						if (group != null)
						{
							Context context = new Context(classResolver, constantResolver, inheritanceChecker, replacementSet, methodOwner, method, insn, frames, logger);
							group.apply(context);
						}
					}
				}
			}

			replacementSet.apply();
		}
		catch (AnalyzerException e)
		{
			logger.log(Level.WARNING, String.format("Processing %s.%s%s failed", methodOwner.name, method.name, method.desc), e);
		}
	}

	@Nullable
	private ConstantGroup findGroup(String methodOwner, MethodNode method, UnpickValue unpickValue)
	{
		ConstantGroup group = null;

		for (int parameterSource : unpickValue.getParameterSources())
		{
			ConstantGroup g = grouper.getMethodParameterGroup(methodOwner, method.name, method.desc, parameterSource);
			if (g != null && group != null && !g.getName().equals(group.getName()))
			{
				ConstantGroup group_f = group;
				logger.log(Level.WARNING, () -> String.format("Conflicting groups %s and %s competing for the same constant", g.getName(), group_f.getName()));
				return null;
			}
			group = g;
		}

		for (IReplacementGenerator.IParameterUsage paramUsage : unpickValue.getParameterUsages())
		{
			ConstantGroup g = processParameterUsage(paramUsage);
			if (g != null && group != null && !g.getName().equals(group.getName()))
			{
				ConstantGroup group_f = group;
				logger.log(Level.WARNING, () -> String.format("Conflicting groups %s and %s competing for the same constant", g.getName(), group_f.getName()));
				return null;
			}
			group = g;
		}

		for (AbstractInsnNode usage : unpickValue.getUsages())
		{
			ConstantGroup g = processUsage(methodOwner, method, usage);
			if (g != null && group != null && !g.getName().equals(group.getName()))
			{
				ConstantGroup group_f = group;
				logger.log(Level.WARNING, () -> String.format("Conflicting groups %s and %s competing for the same constant", g.getName(), group_f.getName()));
				return null;
			}
			group = g;
		}

		return grouper.getDefaultGroup();
	}

	@Nullable
	private ConstantGroup processParameterUsage(IReplacementGenerator.IParameterUsage paramUsage)
	{
		if (paramUsage.getMethodInvocation().getOpcode() == Opcodes.INVOKEDYNAMIC)
		{
			InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) paramUsage.getMethodInvocation();

			if ("java/lang/invoke/LambdaMetafactory".equals(invokeDynamicInsn.bsm.getOwner()) && "metafactory".equals(invokeDynamicInsn.bsm.getName()))
			{
				Handle lambdaMethod = (Handle) invokeDynamicInsn.bsmArgs[1];
				int kind = lambdaMethod.getTag();
				boolean hasThis = kind != Opcodes.H_GETSTATIC && kind != Opcodes.H_PUTSTATIC && kind != Opcodes.H_INVOKESTATIC && kind != Opcodes.H_NEWINVOKESPECIAL;
				int paramIndex = hasThis ? paramUsage.getParamIndex() - 1 : paramUsage.getParamIndex();
				return grouper.getMethodParameterGroup(lambdaMethod.getOwner(), lambdaMethod.getName(), lambdaMethod.getDesc(), paramIndex);
			}

			return null;
		}
		else
		{
			MethodInsnNode methodInsn = (MethodInsnNode) paramUsage.getMethodInvocation();
			return grouper.getMethodParameterGroup(methodInsn.owner, methodInsn.name, methodInsn.desc, paramUsage.getParamIndex());
		}
	}

	@Nullable
	private ConstantGroup processUsage(String methodOwner, MethodNode enclosingMethod, AbstractInsnNode usage)
	{
		if (usage.getType() == AbstractInsnNode.FIELD_INSN)
		{
			FieldInsnNode fieldInsn = (FieldInsnNode) usage;
			return grouper.getFieldGroup(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
		}

		if (usage.getType() == AbstractInsnNode.METHOD_INSN)
		{
			// A method "usage" is from the return type of a method invocation
			MethodInsnNode method = (MethodInsnNode) usage;
			return grouper.getMethodReturnGroup(method.owner, method.name, method.desc);
		}

		if (usage.getOpcode() >= Opcodes.IRETURN && usage.getOpcode() <= Opcodes.RETURN)
		{
			return grouper.getMethodReturnGroup(methodOwner, enclosingMethod.name, enclosingMethod.desc);
		}

		return null;
	}
}

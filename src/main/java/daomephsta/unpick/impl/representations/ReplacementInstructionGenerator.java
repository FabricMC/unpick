package daomephsta.unpick.impl.representations;

import java.util.logging.Logger;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.api.inheritancecheckers.IInheritanceChecker;
import daomephsta.unpick.impl.LegacyExposed;
import daomephsta.unpick.impl.UnpickValue;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;

import daomephsta.unpick.api.constantresolvers.IConstantResolver;

/**
 * @author Daomephsta
 */
@LegacyExposed
public interface ReplacementInstructionGenerator extends IReplacementGenerator
{
	public abstract boolean canReplace(Context context);
	
	/**
	 * Generates replacement instructions for the provided value
	 * @param context TODO
	 */
	public abstract void generateReplacements(Context context);

	@Override
	default void apply(IContext context)
	{
		Context contextImpl = (Context) context;
		if (canReplace(contextImpl))
		{
			generateReplacements(contextImpl);
		}
	}

	/**
	 * Internal, use {@link IReplacementGenerator.IContext} instead
	 */
	@LegacyExposed
	public class Context implements IContext
	{
		private final IClassResolver classResolver;
		private final IConstantResolver constantResolver;
		private final IInheritanceChecker inheritanceChecker;
		private final ReplacementSet replacementSet;
		private final ClassNode containingClass;
		private final MethodNode containingMethod;
		private final AbstractInsnNode target;
		private final Frame<UnpickValue>[] frames;
		private final Logger logger;

		public Context(IClassResolver classResolver, IConstantResolver constantResolver,
					   IInheritanceChecker inheritanceChecker, ReplacementSet replacementSet, ClassNode containingClass,
					   MethodNode containingMethod, AbstractInsnNode target, Frame<UnpickValue>[] frames, Logger logger)
		{
			this.classResolver = classResolver;
			this.constantResolver = constantResolver;
			this.inheritanceChecker = inheritanceChecker;
			this.replacementSet = replacementSet;
			this.containingClass = containingClass;
			this.containingMethod = containingMethod;
			this.target = target;
			this.frames = frames;
			this.logger = logger;
		}

		@Override
		public IClassResolver getClassResolver()
		{
			return classResolver;
		}

		@Override
		@LegacyExposed
		public IConstantResolver getConstantResolver()
		{
			return constantResolver;
		}

		@Override
		public IInheritanceChecker getInheritanceChecker()
		{
			return inheritanceChecker;
		}

		@Override
		public ClassNode getContainingClass()
		{
			return containingClass;
		}

		@Override
		public MethodNode getContainingMethod()
		{
			return containingMethod;
		}

		@Override
		@LegacyExposed
		public ReplacementSet getReplacementSet()
		{
			return replacementSet;
		}

		/**
		 * @deprecated Use {@link #getTarget} instead.
		 */
		@Deprecated
		@LegacyExposed
		public AbstractInsnNode getArgSeed()
		{
			return target;
		}

		@Override
		public AbstractInsnNode getTarget()
		{
			return target;
		}

		@LegacyExposed
		public Frame<UnpickValue> getFrameForInstruction(AbstractInsnNode insn)
		{
			return frames[containingMethod.instructions.indexOf(insn)];
		}

		@SuppressWarnings("unchecked")
		@Nullable
		@Override
		public Frame<IDataflowValue> getDataflowFrame(AbstractInsnNode insn)
		{
			return (Frame<IDataflowValue>) (Frame<?>) frames[containingMethod.instructions.indexOf(insn)];
		}

		@Override
		@LegacyExposed
		public Logger getLogger()
		{
			return logger;
		}
	}
}

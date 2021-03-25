package daomephsta.unpick.impl.representations;

import java.util.logging.Logger;

import daomephsta.unpick.impl.UnpickValue;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.Frame;

import daomephsta.unpick.api.constantresolvers.IConstantResolver;

/**
 * @author Daomephsta
 */
public interface ReplacementInstructionGenerator
{
	public abstract boolean canReplace(Context context);
	
	/**
	 * Generates replacement instructions for the provided value
	 * @param context TODO
	 */
	public abstract void generateReplacements(Context context);
	
	public class Context
	{
		private final IConstantResolver constantResolver;
		private final ReplacementSet replacementSet;
		private final AbstractInsnNode argSeed;
		private final InsnList instructions;
		private final Frame<UnpickValue>[] frames;
		private final Logger logger;

		public Context(IConstantResolver constantResolver, ReplacementSet replacementSet, AbstractInsnNode argSeed,
				InsnList instructions, Frame<UnpickValue>[] frames, Logger logger)
		{
			this.constantResolver = constantResolver;
			this.replacementSet = replacementSet;
			this.argSeed = argSeed;
			this.instructions = instructions;
			this.frames = frames;
			this.logger = logger;
		}

		public IConstantResolver getConstantResolver()
		{
			return constantResolver;
		}

		public ReplacementSet getReplacementSet()
		{
			return replacementSet;
		}

		public AbstractInsnNode getArgSeed()
		{
			return argSeed;
		}
		
		public Frame<UnpickValue> getFrameForInstruction(AbstractInsnNode insn)
		{
			return frames[instructions.indexOf(insn)];
		}
		
		public Logger getLogger()
		{
			return logger;
		}
	}
}

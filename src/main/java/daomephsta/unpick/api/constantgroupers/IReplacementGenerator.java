package daomephsta.unpick.api.constantgroupers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import java.util.Set;
import java.util.logging.Logger;

@FunctionalInterface
public interface IReplacementGenerator
{
	void apply(IContext context);

	@ApiStatus.NonExtendable
	interface IContext
	{
		IClassResolver getClassResolver();
		IConstantResolver getConstantResolver();
		IInheritanceChecker getInheritanceChecker();
		IReplacementSet getReplacementSet();
		ClassNode getContainingClass();
		MethodNode getContainingMethod();
		AbstractInsnNode getTarget();
		@Nullable
		Frame<IDataflowValue> getDataflowFrame(AbstractInsnNode insn);
		Logger getLogger();
	}

	@ApiStatus.NonExtendable
	interface IReplacementSet
	{
		default void addReplacement(AbstractInsnNode oldNode, AbstractInsnNode newNode)
		{
			InsnList newNodes = new InsnList();
			newNodes.add(newNode);
			addReplacement(oldNode, newNodes);
		}

		void addReplacement(AbstractInsnNode oldNode, InsnList newNodes);
	}

	@ApiStatus.NonExtendable
	interface IDataflowValue extends Value
	{
		Type getDataType();
		Set<Integer> getParameterSources();
		Set<IParameterUsage> getParameterUsages();
		Set<AbstractInsnNode> getUsages();
		Set<DataType> getNarrowTypeInterpretations();
	}

	@ApiStatus.NonExtendable
	interface IParameterUsage
	{
		AbstractInsnNode getMethodInvocation();
		int getParamIndex();
	}
}

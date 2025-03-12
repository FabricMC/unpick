package daomephsta.unpick.impl.representations;

import daomephsta.unpick.api.constantgroupers.IReplacementGenerator;
import daomephsta.unpick.impl.Utils;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ReplacementSet implements IReplacementGenerator.IReplacementSet
{
	private final InsnList target;
	private final Map<AbstractInsnNode, InsnList> replacements = new HashMap<>();

	public ReplacementSet(InsnList target)
	{
		this.target = target;
	}

	@Override
	public void addReplacement(AbstractInsnNode oldNode, InsnList newNodes)
	{
		if (replacements.putIfAbsent(oldNode, newNodes) != null)
			throw new IllegalArgumentException("Replacement already defined for " + Utils.visitableToString(oldNode::accept).trim());
	}

	public void apply()
	{
		for (Map.Entry<AbstractInsnNode, InsnList> replacement : replacements.entrySet())
		{
			AbstractInsnNode oldNode = replacement.getKey();
			InsnList newNodes = replacement.getValue();
			target.insert(oldNode, newNodes);
			target.remove(oldNode);
		}
	}

	@Override
	public String toString()
	{
		return String.format("ReplacementSet %s",
				replacements.entrySet().stream().collect
				(
						Collectors.toMap
							(
								e -> Utils.visitableToString(e.getKey()::accept).trim(),
								e -> Utils.visitableToString(e.getValue()::accept).trim()
							)
				));
	}
}

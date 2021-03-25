package daomephsta.unpick.impl;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import daomephsta.unpick.impl.representations.ReplacementSet;

public class FlagStatement
{	
	private final Node root;
	
	private FlagStatement(Node root)
	{
		this.root = root;
	}

	public static Optional<FlagStatement> create(AbstractInsnNode rootInsn, Function<AbstractInsnNode, Frame<UnpickValue>> insnToFrame)
	{
		boolean isBitwiseOp = rootInsn.getOpcode() >= Opcodes.IAND || rootInsn.getOpcode() <= Opcodes.LXOR;
		boolean isLiteral = rootInsn.getOpcode() >= Opcodes.ICONST_M1 && rootInsn.getOpcode() <= Opcodes.LDC;
		if (!isBitwiseOp && !isLiteral)
			throw new IllegalArgumentException("Root node must be a literal or bitwise operator");
		
		return toNode(rootInsn, insnToFrame).map(FlagStatement::new);
	}
	
	private static Optional<Node> toNode(AbstractInsnNode insn, Function<AbstractInsnNode, Frame<UnpickValue>> insnToFrame)
	{
		if (insn.getOpcode() >= Opcodes.IAND && insn.getOpcode() <= Opcodes.LXOR)
		{
			BitOp operation = getOp(insn);
			Frame<UnpickValue> frame = insnToFrame.apply(insn);
			List<Node> args = new ArrayList<>(operation.argCount);
			for (int s = frame.getStackSize() - operation.argCount; s < frame.getStackSize(); s++)
			{
				Set<AbstractInsnNode> insns = frame.getStack(s).getSourceValue().insns;
				if (insns.size() != 1)
					return Optional.empty();
				Optional<Node> node = toNode(insns.iterator().next(), insnToFrame);
				if (node.isPresent())
					args.add(node.get());
				else
					return Optional.empty();
			}
			return Optional.of(new Operation(insn, operation, args));
		}
		else if (insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.LDC)
		{
			Object value = AbstractInsnNodes.getLiteralValue(insn);
			if (value instanceof Integer || value instanceof Long)
				return Optional.of(new Literal(insn, (Number) value));
			else
				throw new IllegalArgumentException(Utils.visitableToString(insn::accept).trim() + " is not an integer literal");
		}
		return Optional.of(new Other(insn));
	}

	private static BitOp getOp(AbstractInsnNode insn)
	{
		if (insn.getOpcode() == Opcodes.IAND || insn.getOpcode() == Opcodes.LAND)
			return BitOp.AND;
		else if (insn.getOpcode() == Opcodes.IOR || insn.getOpcode() == Opcodes.LOR)
			return BitOp.OR;
		else if (insn.getOpcode() == Opcodes.IXOR || insn.getOpcode() == Opcodes.LXOR)
			return BitOp.XOR;
		else
			throw new IllegalArgumentException(Utils.visitableToString(insn::accept).trim() + " is not a bitwise operator");
	} 
	
	public void collectReplacements(ReplacementSet replacementSet, BiFunction<Number, BitOp, Optional<InsnList>> literalConverter)
	{
		collectReplacements(root, BitOp.NONE, replacementSet, literalConverter);
	}
	
	private void collectReplacements(Node node, BitOp bitOp, ReplacementSet replacementSet, BiFunction<Number, BitOp, Optional<InsnList>> literalConverter)
	{
		if (node instanceof Operation)
			collectChildReplacements((Operation) node, replacementSet, literalConverter);
		else if (node instanceof Literal)
		{
			literalConverter.apply(((Literal) node).literal, bitOp)
				.ifPresent(newNodes -> replacementSet.addReplacement(node.source, newNodes));
		}
		else if (node instanceof Other)
		{/*No replacement needed*/}
	}

	private void collectChildReplacements(Operation operation, ReplacementSet replacementSet, BiFunction<Number, BitOp, Optional<InsnList>> literalConverter)
	{
		ListIterator<Node> listIterator = operation.args.listIterator(operation.args.size());
		while(listIterator.hasPrevious())
		{
			collectReplacements(listIterator.previous(), operation.type, replacementSet, literalConverter);
		}
	}

	public static enum BitOp
	{
		AND(2),
		OR(2),
		XOR(2),
		NOT(1),
		NONE(0);
		
		private final int argCount;

		private BitOp(int argCount)
		{
			this.argCount = argCount;
		}
	}
	
	private static class Node 
	{
		private final AbstractInsnNode source;

		protected Node(AbstractInsnNode source)
		{
			this.source = source;
		}
	}
	
	private static class Literal extends Node
	{
		private final Number literal;

		public Literal(AbstractInsnNode source, Number literal)
		{
			super(source);
			this.literal = literal;
		}

		@Override
		public String toString()
		{
			return literal.toString();
		}
	}
	
	private static class Operation extends Node
	{
		private final BitOp type;
		private final List<Node> args;
		
		public Operation(AbstractInsnNode source, BitOp type, List<Node> args)
		{
			super(source);
			this.type = type;
			this.args = args;
		}

		@Override
		public String toString()
		{
			return String.format("Operation [type=%s, args=%s]", type, args);
		}
	}
	
	private static class Other extends Node
	{
		private final AbstractInsnNode node;

		public Other(AbstractInsnNode node)
		{
			super(node);
			/*Can safely pass null, as no node that could end up in a FlagStatement
			* uses the label map*/
			this.node = node.clone(null);
		}

		@Override
		public String toString()
		{
			return String.format("Other [node=%s]", Utils.visitableToString(node::accept).trim());
		}
	}
	
	@Override
	public String toString()
	{
		return String.format("FlagStatement [root=%s]", root);
	}
}

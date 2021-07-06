package daomephsta.unpick.impl.representations;

import java.util.*;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;

import daomephsta.unpick.impl.*;

/**
 * A group of flags represented by {@link FlagDefinition}s.
 * @author Daomephsta
 */
public class FlagConstantGroup extends AbstractConstantGroup<FlagDefinition>
{
	private static final Logger LOGGER = Logger.getLogger("unpick");
	private final Collection<FlagDefinition> resolvedConstantDefinitions = new ArrayList<>();
	
	public FlagConstantGroup(String id)
	{
		super(id);
	}
	
	/**
	 * Adds a flag to this group.
	 * @param flagDefinition a constant definition.
	 */
	@Override
	public void add(FlagDefinition flagDefinition)
	{
		LOGGER.info("Loaded " + flagDefinition + " into '" + getId() + "'");
		if (flagDefinition.isResolved())
			resolvedConstantDefinitions.add(flagDefinition);
		else 
			unresolvedConstantDefinitions.add(flagDefinition);
	}

	@Override
	public boolean canReplace(Context context)
	{
		if (!AbstractInsnNodes.hasLiteralValue(context.getArgSeed()))
			return false;
		Object literalObj = AbstractInsnNodes.getLiteralValue(context.getArgSeed());
		return literalObj instanceof Integer || literalObj instanceof Long;
	}
	
	@Override
	public void generateReplacements(Context context)
	{
		Number literalNum = (Number) AbstractInsnNodes.getLiteralValue(context.getArgSeed());
		IntegerType integerType = IntegerType.from(literalNum);

		long literal = integerType.toUnsignedLong(literalNum);
		if (literal == 0 || literal == -1)
		{
			// Special cases: likely we want just the literal constant, but check for any named constants representing 0 or -1
			for (FlagDefinition constant : resolvedConstantDefinitions)
			{
				if (integerType.toUnsignedLong(constant.getValue()) == literal)
				{
					context.getReplacementSet().addReplacement(context.getArgSeed(), new FieldInsnNode(Opcodes.GETSTATIC, constant.getOwner(), constant.getName(), constant.getDescriptorString()));
					break;
				}
			}
			return;
		}

		List<FlagDefinition> orConstants = new ArrayList<>();
		long orResidual = getConstantsEncompassing(literal, integerType, orConstants);
		long negatedLiteral = integerType.toUnsignedLong(integerType.binaryNegate(literalNum));
		List<FlagDefinition> negatedConstants = new ArrayList<>();
		long negatedResidual = getConstantsEncompassing(negatedLiteral, integerType, negatedConstants);

		boolean negated = negatedResidual == 0 && (orResidual != 0 || negatedConstants.size() < orConstants.size());
		List<FlagDefinition> constants = negated ? negatedConstants : orConstants;
		if (constants.isEmpty())
			return;
		long residual = negated ? negatedResidual : orResidual;

		InsnList replacement = new InsnList();

		boolean firstConstant = true;
		for (FlagDefinition constant : constants)
		{
			replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, constant.getOwner(), constant.getName(), constant.getDescriptorString()));

			if (firstConstant)
				firstConstant = false;
			else
				replacement.add(integerType.createOrInsn());
		}

		if (residual != 0)
		{
			replacement.add(integerType.createLiteralPushInsn(residual));
			replacement.add(integerType.createOrInsn());
		}

		if (negated)
		{
			// bitwise not
			replacement.add(integerType.createLiteralPushInsn(-1));
			replacement.add(integerType.createXorInsn());
		}

		context.getReplacementSet().addReplacement(context.getArgSeed(), replacement);
	}

	/**
	 * Adds the constants that encompass {@code literal} to {@code constantsOut}.
	 * Returns the residual (bits set in the literal not covered by the returned constants).
	 */
	private long getConstantsEncompassing(long literal, IntegerType integerType, List<FlagDefinition> constantsOut)
	{
		long residual = literal;
		for (FlagDefinition constant : resolvedConstantDefinitions)
		{
			long val = integerType.toUnsignedLong(constant.getValue());
			if ((val & residual) != 0 && (val & literal) == val)
			{
				residual &= ~val;
				constantsOut.add(constant);
				if (residual == 0)
					break;
			}
		}
		return residual;
	}
	
	@Override
	protected void acceptResolved(FlagDefinition definition)
	{
		resolvedConstantDefinitions.add(definition);
	}

	@Override
	public String toString()
	{
		return String.format("FlagGroup [Resolved Flag Definitions: %s, Unresolved Flag Definitions: %s]",
			resolvedConstantDefinitions, unresolvedConstantDefinitions);
	}
}

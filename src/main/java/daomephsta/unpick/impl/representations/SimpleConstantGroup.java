package daomephsta.unpick.impl.representations;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;

import daomephsta.unpick.impl.AbstractInsnNodes;

/**
 * A group of constants represented by {@link SimpleConstantDefinition}s.
 * @author Daomephsta
 */
public class SimpleConstantGroup extends AbstractConstantGroup<SimpleConstantDefinition>
{
	private static final Logger LOGGER = Logger.getLogger("unpick");
	private final Map<Object, SimpleConstantDefinition> resolvedConstantDefinitions = new HashMap<>();
	
	public SimpleConstantGroup(String id)
	{
		super(id);
	}
	
	/**
	 * Adds a constant definition to this group.
	 * @param constantDefinition a constant definition.
	 */
	@Override
	public void add(SimpleConstantDefinition constantDefinition)
	{
		LOGGER.info("Loaded " + constantDefinition + " into '" + getId() + "'");
		if (constantDefinition.isResolved())
			acceptResolved(constantDefinition);
		else 
			unresolvedConstantDefinitions.add(constantDefinition);
	}

	@Override
	public boolean canReplace(Context context)
	{
		return AbstractInsnNodes.hasLiteralValue(context.getArgSeed()) 
				&& resolvedConstantDefinitions.containsKey(AbstractInsnNodes.getLiteralValue(context.getArgSeed()));
	}
	
	@Override
	public void generateReplacements(Context context)
	{
		Object literalValue = AbstractInsnNodes.getLiteralValue(context.getArgSeed());
		SimpleConstantDefinition constantDefinition = resolvedConstantDefinitions.get(literalValue);
		context.getReplacementSet().addReplacement(context.getArgSeed(), 
				new FieldInsnNode(Opcodes.GETSTATIC, constantDefinition.getOwner(), 
						constantDefinition.getName(), constantDefinition.getDescriptorString()));
	}
	
	@Override
	protected void acceptResolved(SimpleConstantDefinition definition)
	{
		resolvedConstantDefinitions.put(definition.getValue(), definition);
	}

	@Override
	public String toString()
	{
		return String.format("ConstantGroup [Resolved Constant Definitions: %s, Unresolved Constant Definitions: %s]",
			resolvedConstantDefinitions, unresolvedConstantDefinitions);
	}
}

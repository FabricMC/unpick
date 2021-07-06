package daomephsta.unpick.impl.representations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.impl.representations.AbstractConstantDefinition.ResolutionException;

public abstract class AbstractConstantGroup<T extends AbstractConstantDefinition<T>> implements ReplacementInstructionGenerator
{
	private static final Logger LOGGER = Logger.getLogger("unpick");
	protected final Collection<T> unresolvedConstantDefinitions = new ArrayList<>();
	private final String id;
	
	public AbstractConstantGroup(String id)
	{
		this.id = id;
	}

	/**
	 * Adds a constant definition to this group.
	 * @param constantDefinition a constant definition.
	 */
	public abstract void add(T constantDefinition);
	
	public final void resolveAllConstants(IConstantResolver constantResolver)
	{
	    List<String> errors = new ArrayList<>();;
	    for (Iterator<T> iter = unresolvedConstantDefinitions.iterator(); iter.hasNext();)
	    {
            T definition = iter.next();
            try
            {
                acceptResolved(definition.resolve(constantResolver));
                iter.remove();
            } 
            catch (ResolutionException e)
            {
                errors.add(e.getMessage());
            }
	    }
	    if (!isResolved())
	        LOGGER.severe("Resolution failed for the following constants of group " + id + '\n' + String.join("\n", errors));
	}

    public boolean isResolved()
    {
        return unresolvedConstantDefinitions.isEmpty();
    }
	
	protected abstract void acceptResolved(T definition);
	
	public String getId()
	{
		return id;
	}
}

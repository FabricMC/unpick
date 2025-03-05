package daomephsta.unpick.api.constantgroupers;

import org.jetbrains.annotations.ApiStatus;

public final class ConstantGroup
{
	private final String name;
	private final IReplacementGenerator replacementGenerator;

	public ConstantGroup(String name, IReplacementGenerator replacementGenerator)
	{
		this.name = name;
		this.replacementGenerator = replacementGenerator;
	}

	public String getName()
	{
		return name;
	}

	@ApiStatus.Internal
	public void apply(IReplacementGenerator.IContext context)
	{
		replacementGenerator.apply(context);
	}
}

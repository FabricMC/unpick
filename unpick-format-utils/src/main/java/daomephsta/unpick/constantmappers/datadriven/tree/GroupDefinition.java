package daomephsta.unpick.constantmappers.datadriven.tree;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class GroupDefinition
{
	public final GroupScope scope;
	public final GroupType type;
	public final boolean strict;
	public final DataType dataType;
	@Nullable
	public final String name;
	public final List<GroupConstant> constants;
	@Nullable
	public final GroupFormat format;

	public GroupDefinition(
		GroupScope scope,
		GroupType type,
		boolean strict,
		DataType dataType,
		@Nullable String name,
		List<GroupConstant> constants,
		@Nullable GroupFormat format
	)
	{
		this.scope = scope;
		this.type = type;
		this.strict = strict;
		this.dataType = dataType;
		this.name = name;
		this.constants = constants;
		this.format = format;
	}
}

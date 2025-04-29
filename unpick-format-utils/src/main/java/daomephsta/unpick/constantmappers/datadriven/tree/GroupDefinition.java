package daomephsta.unpick.constantmappers.datadriven.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public final class GroupDefinition {
	public final GroupScope scope;
	public final GroupType type;
	public final boolean strict;
	public final DataType dataType;
	@Nullable
	public final String name;
	public final List<GroupConstant> constants;
	@Nullable
	public final GroupFormat format;

	@ApiStatus.Internal
	public GroupDefinition(
			GroupScope scope,
			GroupType type,
			boolean strict,
			DataType dataType,
			@Nullable String name,
			List<GroupConstant> constants,
			@Nullable GroupFormat format
	) {
		this.scope = scope;
		this.type = type;
		this.strict = strict;
		this.dataType = dataType;
		this.name = name;
		this.constants = constants;
		this.format = format;
	}

	public static final class Builder {
		private GroupScope scope = GroupScope.Global.INSTANCE;
		private GroupType type = GroupType.CONST;
		private boolean strict = false;
		private final DataType dataType;
		@Nullable
		private final String name;
		private final List<GroupConstant> constants = new ArrayList<>();
		@Nullable
		private GroupFormat format = null;

		private Builder(DataType dataType, @Nullable String name) {
			this.dataType = dataType;
			this.name = name;
		}

		public static Builder global(DataType dataType) {
			return new Builder(dataType, null);
		}

		public static Builder named(DataType dataType, String name) {
			return new Builder(dataType, name);
		}

		public static Builder from(GroupDefinition groupDefinition) {
			Builder builder = new Builder(groupDefinition.dataType, groupDefinition.name)
					.scoped(groupDefinition.scope)
					.type(groupDefinition.type)
					.constants(groupDefinition.constants)
					.format(groupDefinition.format);
			builder.strict = groupDefinition.strict;
			return builder;
		}

		public Builder scoped(GroupScope scope) {
			this.scope = scope;
			return this;
		}

		public Builder type(GroupType type) {
			this.type = type;
			return this;
		}

		public Builder strict() {
			this.strict = true;
			return this;
		}

		public Builder constant(GroupConstant constant) {
			this.constants.add(constant);
			return this;
		}

		public Builder constants(Collection<GroupConstant> constants) {
			this.constants.addAll(constants);
			return this;
		}

		public Builder format(GroupFormat format) {
			this.format = format;
			return this;
		}

		public GroupDefinition build() {
			return new GroupDefinition(scope, type, strict, dataType, name, constants, format);
		}
	}
}

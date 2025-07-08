package daomephsta.unpick.constantmappers.datadriven.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;

public record GroupDefinition(
		List<GroupScope> scopes,
		boolean flags,
		boolean strict,
		DataType dataType,
		@Nullable String name,
		List<Expression> constants,
		@Nullable GroupFormat format,
		@Nullable String docs
) {
	@ApiStatus.Internal
	public GroupDefinition {
	}

	public static final class Builder {
		private final List<GroupScope> scopes = new ArrayList<>();
		private boolean flags;
		private boolean strict;
		private final DataType dataType;
		@Nullable
		private final String name;
		private final List<Expression> constants = new ArrayList<>();
		@Nullable
		private GroupFormat format;
		@Nullable
		private String docs;

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

		public static Builder from(GroupDefinition definition) {
			Builder builder = new Builder(definition.dataType(), definition.name())
					.scopes(definition.scopes())
					.constants(definition.constants())
					.format(definition.format())
					.docs(definition.docs());
			builder.flags = definition.flags();
			builder.strict = definition.strict();
			return builder;
		}

		public Builder scope(GroupScope scope) {
			this.scopes.add(scope);
			return this;
		}

		public Builder scopes(Collection<? extends GroupScope> scopes) {
			this.scopes.addAll(scopes);
			return this;
		}

		public Builder setScopes(Collection<? extends GroupScope> scopes) {
			this.scopes.clear();
			this.scopes.addAll(scopes);
			return this;
		}

		public Builder flags() {
			this.flags = true;
			return this;
		}

		public Builder strict() {
			this.strict = true;
			return this;
		}

		public Builder constant(Expression constant) {
			this.constants.add(constant);
			return this;
		}

		public Builder constants(Collection<? extends Expression> constants) {
			this.constants.addAll(constants);
			return this;
		}

		public Builder setConstants(Collection<? extends Expression> constants) {
			this.constants.clear();
			this.constants.addAll(constants);
			return this;
		}

		public Builder format(GroupFormat format) {
			this.format = format;
			return this;
		}

		public Builder docs(String docs) {
			this.docs = docs;
			return this;
		}

		public GroupDefinition build() {
			return new GroupDefinition(scopes, flags, strict, dataType, name, constants, format, docs);
		}
	}
}

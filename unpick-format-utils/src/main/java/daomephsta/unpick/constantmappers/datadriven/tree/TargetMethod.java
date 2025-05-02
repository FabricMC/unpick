package daomephsta.unpick.constantmappers.datadriven.tree;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public record TargetMethod(
		String className,
		String methodName,
		String methodDesc,
		Map<Integer, String> paramGroups,
		@Nullable String returnGroup
) {
	public static final class Builder {
		private final String className;
		private final String methodName;
		private final String methodDesc;
		private final Map<Integer, String> paramGroups = new HashMap<>();
		@Nullable
		private String returnGroup;

		private Builder(String className, String methodName, String methodDesc) {
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}

		public static Builder builder(String className, String methodName, String methodDesc) {
			return new Builder(className, methodName, methodDesc);
		}

		public static Builder from(TargetMethod method) {
			return new Builder(method.className(), method.methodName(), method.methodDesc())
					.paramGroups(method.paramGroups())
					.returnGroup(method.returnGroup());
		}

		public Builder paramGroup(int index) {
			return paramGroup(index, null);
		}

		public Builder paramGroup(int index, String group) {
			this.paramGroups.put(index, group);
			return this;
		}

		public Builder paramGroups(Map<Integer, String> paramGroups) {
			this.paramGroups.putAll(paramGroups);
			return this;
		}

		public Builder setParamGroups(Map<Integer, String> paramGroups) {
			this.paramGroups.clear();
			this.paramGroups.putAll(paramGroups);
			return this;
		}

		public Builder returnGroup(String returnGroup) {
			this.returnGroup = returnGroup;
			return this;
		}

		public TargetMethod build() {
			return new TargetMethod(className, methodName, methodDesc, Map.copyOf(paramGroups), returnGroup);
		}
	}
}

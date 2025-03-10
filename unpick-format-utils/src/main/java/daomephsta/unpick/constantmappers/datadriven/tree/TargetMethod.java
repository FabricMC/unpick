package daomephsta.unpick.constantmappers.datadriven.tree;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class TargetMethod
{
	public final String className;
	public final String methodName;
	public final String methodDesc;
	public final Map<Integer, String> paramGroups;
	@Nullable
	public final String returnGroup;

	public TargetMethod(
		String className,
		String methodName,
		String methodDesc,
		Map<Integer, String> paramGroups,
		@Nullable String returnGroup
	)
	{
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.paramGroups = paramGroups;
		this.returnGroup = returnGroup;
	}

	public static final class Builder
	{
		private final String className;
		private final String methodName;
		private final String methodDesc;
		private final Map<Integer, String> paramGroups = new HashMap<>();
		@Nullable
		private String returnGroup;

		private Builder(String className, String methodName, String methodDesc)
		{
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}

		public static Builder  builder(String className, String methodName, String methodDesc)
		{
			return new Builder(className, methodName, methodDesc);
		}

		public Builder paramGroup(int index, String group)
		{
			paramGroups.put(index, group);
			return this;
		}

		public Builder returnGroup(String group)
		{
			returnGroup = group;
			return this;
		}

		public TargetMethod build()
		{
			return new TargetMethod(className, methodName, methodDesc, paramGroups, returnGroup);
		}
	}
}

package daomephsta.unpick.constantmappers.datadriven.tree;

import org.jetbrains.annotations.Nullable;

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
}

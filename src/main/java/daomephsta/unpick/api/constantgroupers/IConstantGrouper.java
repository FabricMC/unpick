package daomephsta.unpick.api.constantgroupers;

import org.jetbrains.annotations.Nullable;

public interface IConstantGrouper
{
	@Nullable
	default ConstantGroup getFieldGroup(String fieldOwner, String fieldName, String fieldDescriptor)
	{
		return null;
	}

	@Nullable
	default ConstantGroup getMethodReturnGroup(String methodOwner, String methodName, String methodDescriptor)
	{
		return null;
	}

	@Nullable
	default ConstantGroup getMethodParameterGroup(String methodOwner, String methodName, String methodDescriptor, int parameterIndex)
	{
		return null;
	}

	@Nullable
	default ConstantGroup getDefaultGroup()
	{
		return null;
	}
}

package daomephsta.unpick.api.constantmappers;

import daomephsta.unpick.api.constantgroupers.ConstantGroup;
import daomephsta.unpick.api.constantgroupers.IConstantGrouper;
import daomephsta.unpick.impl.representations.ReplacementInstructionGenerator.Context;
import org.jetbrains.annotations.Nullable;

/**
 * Maps inlined values to replacement instructions
 * @author Daomephsta
 *
 * @deprecated Use {@link IConstantGrouper} instead.
 */
@Deprecated
public interface IConstantMapper extends IConstantGrouper
{
	/**
	 * @param methodOwner the internal name of the class that owns the method.
	 * @param methodName the name of the method.
	 * @param methodDescriptor the descriptor of the method.
	 * @return true if this mapper targets the method.
	 */
	public boolean targets(String methodOwner, String methodName, String methodDescriptor);
	
	/**
	 * @param methodOwner the internal name of the class that owns the method.
	 * @param methodName the name of the method.
	 * @param methodDescriptor the descriptor of the method.
	 * @param parameterIndex the index of the parameter being checked
	 * @return true if this mapper targets the parameter of the method with a 
	 * parameter index of {@code parameterIndex}.
	 */
	public boolean targetsParameter(String methodOwner, String methodName, String methodDescriptor, int parameterIndex);
	
	/**
	 * Maps an inlined parameter value to replacement instructions, for a given target method. 
	 * @param methodOwner the internal name of the class that owns the target method.
	 * @param methodName the name of the target method.
	 * @param methodDescriptor the descriptor of the target method.
	 * @param parameterIndex the index of the parameter of the target method that {@code value} is passed to.
	 * @param context the context of the replacement
	 */
	public void mapParameter(String methodOwner, String methodName, String methodDescriptor, int parameterIndex, Context context);

	/**
	 * @param methodOwner the internal name of the class that owns the method.
	 * @param methodName the name of the method.
	 * @param methodDescriptor the descriptor of the method.
	 * @return true if this mapper targets return statements of the method.
	 */
	public boolean targetsReturn(String methodOwner, String methodName, String methodDescriptor);

	/**
	 * Maps an inlined return value to replacement instructions, for a given target method. 
	 * @param methodOwner the internal name of the class that owns the target method.
	 * @param methodName the name of the target method.
	 * @param methodDescriptor the descriptor of the target method.
	 * @param context the context of the replacement.
	 */
	public void mapReturn(String methodOwner, String methodName, String methodDescriptor, Context context);

	@Override
	@Nullable
	default ConstantGroup getMethodReturnGroup(String methodOwner, String methodName, String methodDescriptor)
	{
		if (targets(methodOwner, methodName, methodDescriptor) && targetsReturn(methodOwner, methodName, methodDescriptor))
		{
			return new ConstantGroup("<unknown group>", context -> mapReturn(methodOwner, methodName, methodDescriptor, (Context) context));
		}

		return null;
	}

	@Override
	@Nullable
	default ConstantGroup getMethodParameterGroup(String methodOwner, String methodName, String methodDescriptor, int parameterIndex)
	{
		if (targets(methodOwner, methodName, methodDescriptor) && targetsParameter(methodOwner, methodName, methodDescriptor, parameterIndex))
		{
			return new ConstantGroup("<unknown group>", context -> mapParameter(methodOwner, methodName, methodDescriptor, parameterIndex, (Context) context));
		}

		return null;
	}
}

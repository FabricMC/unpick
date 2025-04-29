package daomephsta.unpick.impl.constantmappers;

import java.util.Map;
import java.util.logging.Level;

import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.impl.representations.ReplacementInstructionGenerator;
import daomephsta.unpick.impl.representations.ReplacementInstructionGenerator.Context;
import daomephsta.unpick.impl.representations.TargetMethods;

public abstract class SimpleAbstractConstantMapper implements IConstantMapper {
	protected final Map<String, ReplacementInstructionGenerator> constantGroups;

	protected SimpleAbstractConstantMapper(Map<String, ReplacementInstructionGenerator> constantGroups) {
		this.constantGroups = constantGroups;
	}

	protected abstract TargetMethods getTargetMethods();

	@Override
	public boolean targets(String methodOwner, String methodName, String methodDescriptor) {
		return getTargetMethods().targets(methodOwner, methodName, methodDescriptor);
	}

	@Override
	public boolean targetsParameter(String methodOwner, String methodName, String methodDescriptor, int parameterIndex) {
		return getTargetMethods().targetsParameter(methodOwner, methodName, methodDescriptor, parameterIndex);
	}

	@Override
	public void mapParameter(String methodOwner, String methodName, String methodDescriptor, int parameterIndex, Context context) {
		String constantGroupID = getTargetMethods().getParameterConstantGroup(methodOwner, methodName, methodDescriptor, parameterIndex);
		ReplacementInstructionGenerator constantGroup = constantGroups.get(constantGroupID);
		if (constantGroup == null) {
			throw new UnpickSyntaxException(String.format("The constant group '%s' does not exist. Target: %s.%s%s parameter %d",
				constantGroupID, methodOwner, methodName, methodDescriptor, parameterIndex));
		}
		if (!constantGroup.canReplace(context)) {
			context.getLogger().log(Level.INFO, "Transformation skipped. Constant group '%s' cannot transform this invocation.", constantGroupID);
			return;
		}

		constantGroup.generateReplacements(context);
		context.getLogger().log(Level.INFO, "Transformation complete");
	}

	@Override
	public boolean targetsReturn(String methodOwner, String methodName, String methodDescriptor) {
		return getTargetMethods().targetsReturn(methodOwner, methodName, methodDescriptor);
	}

	@Override
	public void mapReturn(String methodOwner, String methodName, String methodDescriptor, Context context) {
		String constantGroupID = getTargetMethods().getReturnConstantGroup(methodOwner, methodName, methodDescriptor);
		ReplacementInstructionGenerator constantGroup = constantGroups.get(constantGroupID);
		if (constantGroup == null) {
			throw new UnpickSyntaxException(String.format("The constant group '%s' does not exist. Target: %s.%s%s returns",
				constantGroupID, methodOwner, methodName, methodDescriptor));
		}
		if (!constantGroup.canReplace(context)) {
			context.getLogger().log(Level.INFO, "Transformation skipped. Constant group '%s' cannot transform this invocation.", constantGroupID);
			return;
		}

		constantGroup.generateReplacements(context);
		context.getLogger().log(Level.INFO, "Transformation complete");
	}
}

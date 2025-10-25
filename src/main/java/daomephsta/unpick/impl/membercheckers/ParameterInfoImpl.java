package daomephsta.unpick.impl.membercheckers;

import java.util.List;

import daomephsta.unpick.api.classresolvers.IMemberChecker;

public record ParameterInfoImpl(int access, List<String> annotations) implements IMemberChecker.ParameterInfo {
	@Override
	public IMemberChecker.ParameterInfo withAnnotations(List<String> annotations) {
		return new ParameterInfoImpl(access, annotations);
	}
}

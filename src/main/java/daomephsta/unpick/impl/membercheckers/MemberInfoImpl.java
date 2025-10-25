package daomephsta.unpick.impl.membercheckers;

import java.util.List;

import daomephsta.unpick.api.classresolvers.IMemberChecker;

public record MemberInfoImpl(int access, String name, String desc, List<String> annotations) implements IMemberChecker.MemberInfo {
	@Override
	public IMemberChecker.MemberInfo withAnnotations(List<String> annotations) {
		return new MemberInfoImpl(access, name, desc, annotations);
	}
}

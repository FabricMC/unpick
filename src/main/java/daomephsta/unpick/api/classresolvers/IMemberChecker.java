package daomephsta.unpick.api.classresolvers;

import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.membercheckers.ChainMemberChecker;
import daomephsta.unpick.impl.membercheckers.MemberInfoImpl;
import daomephsta.unpick.impl.membercheckers.ParameterInfoImpl;

public interface IMemberChecker {
	@Nullable("if the class doesn't exist")
	List<MemberInfo> getFields(String className);

	@Nullable("if the class doesn't exist")
	List<MemberInfo> getMethods(String className);

	@Nullable
	default MemberInfo getField(String className, String fieldName, String fieldDesc) {
		List<MemberInfo> fields = getFields(className);
		if (fields == null) {
			return null;
		}

		for (MemberInfo memberInfo : fields) {
			if (memberInfo.name().equals(fieldName) && memberInfo.desc().equals(fieldDesc)) {
				return memberInfo;
			}
		}

		return null;
	}

	@Nullable
	default MemberInfo getMethod(String className, String methodName, String methodDesc) {
		List<MemberInfo> methods = getMethods(className);
		if (methods == null) {
			return null;
		}

		for (MemberInfo memberInfo : methods) {
			if (memberInfo.name().equals(methodName) && memberInfo.desc().equals(methodDesc)) {
				return memberInfo;
			}
		}

		return null;
	}

	@Nullable
	ParameterInfo getParameter(String className, String methodName, String methodDesc, int parameterIndex);

	default IMemberChecker chain(IMemberChecker... others) {
		return new ChainMemberChecker(Utils.prepend(this, others));
	}

	@ApiStatus.NonExtendable
	interface MemberInfo {
		int access();
		String name();
		String desc();
		List<String> annotations();

		static MemberInfo create(int access, String name, String desc) {
			return new MemberInfoImpl(access, name, desc, List.of());
		}

		MemberInfo withAnnotations(List<String> annotations);
	}

	@ApiStatus.NonExtendable
	interface ParameterInfo {
		int access();
		List<String> annotations();

		static ParameterInfo create(int access) {
			return new ParameterInfoImpl(access, List.of());
		}

		ParameterInfo withAnnotations(List<String> annotations);
	}
}

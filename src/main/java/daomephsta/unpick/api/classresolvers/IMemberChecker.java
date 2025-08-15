package daomephsta.unpick.api.classresolvers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.membercheckers.ChainMemberChecker;

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
			if (memberInfo.name.equals(fieldName) && memberInfo.desc.equals(fieldDesc)) {
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
			if (memberInfo.name.equals(methodName) && memberInfo.desc.equals(methodDesc)) {
				return memberInfo;
			}
		}

		return null;
	}

	default IMemberChecker chain(IMemberChecker... others) {
		return new ChainMemberChecker(Utils.prepend(this, others));
	}

	record MemberInfo(int access, String name, String desc) {
	}
}

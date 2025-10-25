package daomephsta.unpick.impl.membercheckers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.api.classresolvers.IMemberChecker;
import daomephsta.unpick.impl.Utils;

public class ChainMemberChecker implements IMemberChecker {
	private final IMemberChecker[] checkers;

	public ChainMemberChecker(IMemberChecker[] checkers) {
		this.checkers = checkers;
	}

	@Override
	@Nullable
	public List<MemberInfo> getFields(String className) {
		for (IMemberChecker checker : checkers) {
			List<MemberInfo> fields = checker.getFields(className);
			if (fields != null) {
				return fields;
			}
		}

		return null;
	}

	@Override
	@Nullable
	public List<MemberInfo> getMethods(String className) {
		for (IMemberChecker checker : checkers) {
			List<MemberInfo> methods = checker.getMethods(className);
			if (methods != null) {
				return methods;
			}
		}

		return null;
	}

	@Override
	@Nullable
	public MemberInfo getField(String className, String fieldName, String fieldDesc) {
		for (IMemberChecker checker : checkers) {
			MemberInfo field = checker.getField(className, fieldName, fieldDesc);
			if (field != null) {
				return field;
			}
		}

		return null;
	}

	@Override
	@Nullable
	public MemberInfo getMethod(String className, String methodName, String methodDesc) {
		for (IMemberChecker checker : checkers) {
			MemberInfo method = checker.getMethod(className, methodName, methodDesc);
			if (method != null) {
				return method;
			}
		}

		return null;
	}

	@Override
	@Nullable
	public ParameterInfo getParameter(String className, String methodName, String methodDesc, int parameterIndex) {
		for (IMemberChecker checker : checkers) {
			ParameterInfo parameter = checker.getParameter(className, methodName, methodDesc, parameterIndex);
			if (parameter != null) {
				return parameter;
			}
		}

		return null;
	}

	@Override
	public IMemberChecker chain(IMemberChecker... others) {
		return new ChainMemberChecker(Utils.concat(checkers, others));
	}
}

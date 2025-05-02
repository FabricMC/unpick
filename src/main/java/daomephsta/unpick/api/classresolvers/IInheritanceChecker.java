package daomephsta.unpick.api.classresolvers;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.impl.inheritancecheckers.ChainInheritanceChecker;

public interface IInheritanceChecker {
	@Nullable
	ClassInfo getClassInfo(String className);

	default boolean isAssignableFrom(String type1, String type2) {
		if ("java/lang/Object".equals(type1)) {
			return true;
		}

		while (type2 != null) {
			if (type1.equals(type2)) {
				return true;
			}

			IInheritanceChecker.ClassInfo classInfo = getClassInfo(type2);
			if (classInfo == null) {
				return false;
			}

			for (String itf : classInfo.interfaces()) {
				if (isAssignableFrom(type1, itf)) {
					return true;
				}
			}

			type2 = classInfo.superClass();
		}

		return false;
	}

	default IInheritanceChecker chain(IInheritanceChecker... others) {
		return new ChainInheritanceChecker(Utils.prepend(this, others));
	}

	record ClassInfo(@Nullable("null in the case of java.lang.Object") String superClass, String[] interfaces, boolean isInterface) {
	}
}

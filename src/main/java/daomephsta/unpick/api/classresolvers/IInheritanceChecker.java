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

			for (String itf : classInfo.getInterfaces()) {
				if (isAssignableFrom(type1, itf)) {
					return true;
				}
			}

			type2 = classInfo.getSuperClass();
		}

		return false;
	}

	default IInheritanceChecker chain(IInheritanceChecker... others) {
		return new ChainInheritanceChecker(Utils.prepend(this, others));
	}

	final class ClassInfo {
		private final String superClass;
		private final String[] interfaces;
		private final boolean isInterface;

		public ClassInfo(String superClass, String[] interfaces, boolean isInterface) {
			this.superClass = superClass;
			this.interfaces = interfaces;
			this.isInterface = isInterface;
		}

		@Nullable("null in the case of java.lang.Object")
		public String getSuperClass() {
			return superClass;
		}

		public String[] getInterfaces() {
			return interfaces;
		}

		public boolean isInterface() {
			return isInterface;
		}
	}
}

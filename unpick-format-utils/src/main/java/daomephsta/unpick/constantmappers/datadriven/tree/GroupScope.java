package daomephsta.unpick.constantmappers.datadriven.tree;

public abstract class GroupScope {
	private GroupScope() {
	}

	public static final class Global extends GroupScope {
		public static final Global INSTANCE = new Global();

		private Global() {
		}
	}

	public static final class Package extends GroupScope {
		public final String packageName;

		public Package(String packageName) {
			this.packageName = packageName;
		}
	}

	public static final class Class extends GroupScope {
		public final String className;

		public Class(String className) {
			this.className = className;
		}
	}

	public static final class Method extends GroupScope {
		public final String className;
		public final String methodName;
		public final String methodDesc;

		public Method(String className, String methodName, String methodDesc) {
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}
	}
}

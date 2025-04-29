package daomephsta.unpick.tests.lib;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodFetcher {
	public static MethodNode fetch(ClassNode clazz, String name, String descriptor) {
		return fetch(clazz, name + descriptor);
	}

	public static MethodNode fetch(ClassNode clazz, String signature) {
		for (MethodNode method : clazz.methods) {
			if (signature.equals(method.name + method.desc)) {
				return method;
			}
		}
		throw new IllegalArgumentException(signature + " does not exist in " + clazz.name);
	}
}

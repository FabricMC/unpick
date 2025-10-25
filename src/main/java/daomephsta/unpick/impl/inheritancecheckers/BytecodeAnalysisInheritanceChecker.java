package daomephsta.unpick.impl.inheritancecheckers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;

public class BytecodeAnalysisInheritanceChecker implements IInheritanceChecker {
	private static final String[] EMPTY_ARRAY = new String[0];

	private final IClassResolver classResolver;
	private final ConcurrentMap<String, ClassInfo> classInfoCache = new ConcurrentHashMap<>();

	public BytecodeAnalysisInheritanceChecker(IClassResolver classResolver) {
		this.classResolver = classResolver;
	}

	@Override
	@Nullable
	public ClassInfo getClassInfo(String className) {
		return classInfoCache.computeIfAbsent(className, name -> {
			ClassNode node = classResolver.resolveClass(name);
			if (node == null) {
				return null;
			}

			return new ClassInfo(node.superName, node.interfaces == null ? EMPTY_ARRAY : node.interfaces.toArray(new String[0]), (node.access & Opcodes.ACC_INTERFACE) != 0);
		});
	}
}

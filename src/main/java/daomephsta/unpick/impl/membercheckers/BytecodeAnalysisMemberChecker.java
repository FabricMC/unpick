package daomephsta.unpick.impl.membercheckers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IMemberChecker;

public class BytecodeAnalysisMemberChecker implements IMemberChecker {
	private final IClassResolver classResolver;
	private final ConcurrentMap<String, ClassInfo> classInfoCache = new ConcurrentHashMap<>();

	public BytecodeAnalysisMemberChecker(IClassResolver classResolver) {
		this.classResolver = classResolver;
	}

	@Override
	@Nullable
	public List<MemberInfo> getFields(String className) {
		ClassInfo classInfo = getClassInfo(className);
		return classInfo != null ? classInfo.fields : null;
	}

	@Override
	@Nullable
	public List<MemberInfo> getMethods(String className) {
		ClassInfo classInfo = getClassInfo(className);
		return classInfo != null ? classInfo.methods : null;
	}

	@Nullable
	private ClassInfo getClassInfo(String className) {
		return classInfoCache.computeIfAbsent(className, k -> {
			ClassNode node = classResolver.resolveClass(k);
			if (node == null) {
				return null;
			}

			List<MemberInfo> fields = new ArrayList<>();
			for (FieldNode field : node.fields) {
				fields.add(new MemberInfo(field.access, field.name, field.desc));
			}

			List<MemberInfo> methods = new ArrayList<>();
			for (MethodNode method : node.methods) {
				methods.add(new MemberInfo(method.access, method.name, method.desc));
			}

			return new ClassInfo(fields, methods);
		});
	}

	private record ClassInfo(List<MemberInfo> fields, List<MemberInfo> methods) {
	}
}

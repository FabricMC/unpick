package daomephsta.unpick.impl.membercheckers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

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
			ClassNode node = classResolver.resolveClassNode(k, 0);
			if (node != null) {
				List<MemberInfo> fields = new ArrayList<>();
				for (var field : node.fields) {
					fields.add(new MemberInfo(field.access, field.name, field.desc));
				}

				List<MemberInfo> methods = new ArrayList<>();
				for (var method : node.methods) {
					methods.add(new MemberInfo(method.access, method.name, method.desc));
				}

				return new ClassInfo(fields, methods);
			}

			ClassReader classReader = classResolver.resolveClass(k);
			if (classReader == null) {
				return null;
			}

			List<MemberInfo> fields = new ArrayList<>();
			List<MemberInfo> methods = new ArrayList<>();

			classReader.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
					fields.add(new MemberInfo(access, name, descriptor));
					return null;
				}

				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					methods.add(new MemberInfo(access, name, descriptor));
					return null;
				}
			}, ClassReader.SKIP_CODE);

			return new ClassInfo(fields, methods);
		});
	}

	private record ClassInfo(List<MemberInfo> fields, List<MemberInfo> methods) {
	}
}

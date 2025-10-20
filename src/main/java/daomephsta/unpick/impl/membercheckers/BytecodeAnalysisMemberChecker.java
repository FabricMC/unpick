package daomephsta.unpick.impl.membercheckers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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

	@Override
	@Nullable
	public ParameterInfo getParameter(String className, String methodName, String methodDesc, int parameterIndex) {
		ClassInfo classInfo = getClassInfo(className);
		if (classInfo == null) {
			return null;
		}

		List<ParameterInfo> params = classInfo.parameters.get(methodName + methodDesc);
		return params != null && parameterIndex < params.size() ? params.get(parameterIndex) : null;
	}

	@Nullable
	private ClassInfo getClassInfo(String className) {
		return classInfoCache.computeIfAbsent(className, k -> {
			ClassReader classReader = classResolver.resolveClass(k);
			if (classReader == null) {
				return null;
			}

			List<MemberInfo> fields = new ArrayList<>();
			List<MemberInfo> methods = new ArrayList<>();
			Map<String, List<ParameterInfo>> parameters = new HashMap<>();

			classReader.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
					return new FieldVisitor(Opcodes.ASM9) {
						final List<String> annotations = new ArrayList<>();

						@Override
						public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
							annotations.add(Type.getType(descriptor).getInternalName());
							return null;
						}

						@Override
						public void visitEnd() {
							fields.add(MemberInfo.create(access, name, descriptor).withAnnotations(annotations));
						}
					};
				}

				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					return new MethodVisitor(Opcodes.ASM9) {
						final List<ParameterInfo> parameterInfos = new ArrayList<>();
						final List<String> annotations = new ArrayList<>();

						@Override
						public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
							annotations.add(Type.getType(descriptor).getInternalName());
							return null;
						}

						@Override
						public void visitParameter(String name, int access) {
							parameterInfos.add(ParameterInfo.create(access));
						}

						@Override
						public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
							for (int i = 0; i <= parameter && i < parameterInfos.size(); i++) {
								if ((parameterInfos.get(i).access() & Opcodes.ACC_SYNTHETIC) != 0) {
									parameter++;
								}
							}

							while (parameter >= parameterInfos.size()) {
								parameterInfos.add(ParameterInfo.create(0));
							}

							ParameterInfo param = parameterInfos.get(parameter);
							List<String> annotations = new ArrayList<>(param.annotations());
							annotations.add(Type.getType(descriptor).getInternalName());
							parameterInfos.set(parameter, param.withAnnotations(annotations));

							return null;
						}

						@Override
						public void visitEnd() {
							methods.add(MemberInfo.create(access, name, descriptor).withAnnotations(annotations));
							parameters.put(name + descriptor, parameterInfos);
						}
					};
				}
			}, ClassReader.SKIP_CODE);

			return new ClassInfo(fields, methods, parameters);
		});
	}

	private record ClassInfo(List<MemberInfo> fields, List<MemberInfo> methods, Map<String, List<ParameterInfo>> parameters) {
	}
}

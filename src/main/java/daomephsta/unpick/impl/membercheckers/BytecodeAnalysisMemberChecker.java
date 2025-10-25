package daomephsta.unpick.impl.membercheckers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IMemberChecker;

public class BytecodeAnalysisMemberChecker implements IMemberChecker {
	@SuppressWarnings("unchecked")
	private static final List<AnnotationNode>[] EMPTY_ANNOTATION_LIST_ARRAY = new List[0];

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
			ClassNode node = classResolver.resolveClass(k);
			if (node == null) {
				return null;
			}

			List<MemberInfo> fields = new ArrayList<>();
			for (FieldNode field : node.fields) {
				fields.add(MemberInfo.create(field.access, field.name, field.desc).withAnnotations(getAnnotations(field.visibleAnnotations, field.invisibleAnnotations)));
			}

			List<MemberInfo> methods = new ArrayList<>();
			Map<String, List<ParameterInfo>> parameters = new HashMap<>();
			for (MethodNode method : node.methods) {
				methods.add(MemberInfo.create(method.access, method.name, method.desc).withAnnotations(getAnnotations(method.visibleAnnotations, method.invisibleAnnotations)));

				List<ParameterInfo> params = new ArrayList<>();
				parameters.put(method.name + method.desc, params);

				List<ParameterNode> paramNodes = Objects.requireNonNullElse(method.parameters, List.of());
				List<AnnotationNode>[] visibleParamAnnotations = Objects.requireNonNullElse(method.visibleParameterAnnotations, EMPTY_ANNOTATION_LIST_ARRAY);
				List<AnnotationNode>[] invisibleParamAnnotations = Objects.requireNonNullElse(method.invisibleParameterAnnotations, EMPTY_ANNOTATION_LIST_ARRAY);
				int realParamIndex = 0;
				int nonSyntheticParamIndex = 0;
				while (nonSyntheticParamIndex < Math.max(visibleParamAnnotations.length, invisibleParamAnnotations.length)) {
					while (realParamIndex < paramNodes.size() && (paramNodes.get(realParamIndex).access & Opcodes.ACC_SYNTHETIC) != 0) {
						params.add(ParameterInfo.create(0));
						realParamIndex++;
					}

					int access = realParamIndex < paramNodes.size() ? paramNodes.get(realParamIndex).access : 0;
					List<AnnotationNode> visibleAnnotations = nonSyntheticParamIndex < visibleParamAnnotations.length ? visibleParamAnnotations[nonSyntheticParamIndex] : null;
					List<AnnotationNode> invisibleAnnotations = nonSyntheticParamIndex < invisibleParamAnnotations.length ? invisibleParamAnnotations[nonSyntheticParamIndex] : null;

					params.add(ParameterInfo.create(access).withAnnotations(getAnnotations(visibleAnnotations, invisibleAnnotations)));

					nonSyntheticParamIndex++;
					realParamIndex++;
				}
			}

			return new ClassInfo(fields, methods, parameters);
		});
	}

	private static List<String> getAnnotations(@Nullable List<AnnotationNode> visibleAnnotations, @Nullable List<AnnotationNode> invisibleAnnotations) {
		List<String> annotations = new ArrayList<>();

		if (visibleAnnotations != null) {
			for (AnnotationNode annotation : visibleAnnotations) {
				annotations.add(Type.getType(annotation.desc).getClassName());
			}
		}

		if (invisibleAnnotations != null) {
			for (AnnotationNode annotation : invisibleAnnotations) {
				annotations.add(Type.getType(annotation.desc).getClassName());
			}
		}

		return annotations;
	}

	private record ClassInfo(List<MemberInfo> fields, List<MemberInfo> methods, Map<String, List<ParameterInfo>> parameters) {
	}
}

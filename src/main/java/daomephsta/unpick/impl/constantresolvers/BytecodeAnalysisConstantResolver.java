package daomephsta.unpick.impl.constantresolvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.impl.AbstractInsnNodes;

/**
 * Resolves constants by analysing the bytecode of their owners.
 * @author Daomephsta
 */
public class BytecodeAnalysisConstantResolver implements IConstantResolver {
	static final Set<Type> VALID_CONSTANT_TYPES = Set.of(
			Type.BYTE_TYPE,
			Type.SHORT_TYPE,
			Type.CHAR_TYPE,
			Type.INT_TYPE,
			Type.LONG_TYPE,
			Type.FLOAT_TYPE,
			Type.DOUBLE_TYPE,
			Type.getObjectType("java/lang/String"),
			Type.getObjectType("java/lang/Class")
	);

	private final ConcurrentMap<String, Map<String, ResolvedConstant>> constantDataCache = new ConcurrentHashMap<>();
	private final IClassResolver classResolver;

	public BytecodeAnalysisConstantResolver(IClassResolver classResolver) {
		this.classResolver = classResolver;
	}

	@Override
	public ResolvedConstant resolveConstant(String owner, String name) {
		Map<String, ResolvedConstant> resolvedConstants = constantDataCache.computeIfAbsent(owner, this::extractConstants);
		return resolvedConstants == null ? null : resolvedConstants.get(name);
	}

	@Override
	public Map<String, ResolvedConstant> getAllConstantsInClass(String owner) {
		Map<String, ResolvedConstant> resolvedConstants = constantDataCache.computeIfAbsent(owner, this::extractConstants);
		return resolvedConstants == null ? null : Collections.unmodifiableMap(resolvedConstants);
	}

	@Nullable
	protected Map<String, ResolvedConstant> extractConstants(String owner) {
		ClassNode node = classResolver.resolveClassNode(owner, 0);
		if (node != null) {
			ResolvedConstantsBuilder builder = new ResolvedConstantsBuilder(owner);
			node.accept(builder);
			return builder.resolvedConstants;
		}
		ClassReader cr = classResolver.resolveClass(owner);
		if (cr == null) {
			return null;
		}
		ResolvedConstantsBuilder builder = new ResolvedConstantsBuilder(owner);
		cr.accept(builder, 0);
		return builder.resolvedConstants;
	}

	// Tries to get the constant value out of the ConstantValue attribute. If the constant is a Class, or null, javac
	// doesn't seem to store it there, so we fall back to scraping the constants out of the <clinit> or <init> methods.
	// In both cases, all assignments to the constant must be an instruction representing the same constant value. For
	// non-static constants, all constructors not delegating via this() must assign the constant.
	private static class ResolvedConstantsBuilder extends ClassVisitor {
		private final String ownerClass;
		private final Map<String, ResolvedConstant> resolvedConstants = new HashMap<>();
		private final Map<String, Type> staticFieldsNeedingValue = new HashMap<>();
		private final Map<String, Type> nonStaticFieldsNeedingValue = new HashMap<>();
		private final List<MethodNode> constructors = new ArrayList<>();
		@Nullable
		private MethodNode staticInitializer;

		ResolvedConstantsBuilder(String ownerClass) {
			super(Opcodes.ASM9);
			this.ownerClass = ownerClass;
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if ((access & Opcodes.ACC_FINAL) == 0) {
				return null;
			}
			Type fieldType = Type.getType(descriptor);
			if (!VALID_CONSTANT_TYPES.contains(fieldType)) {
				return null;
			}

			boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;

			if (value != null) {
				resolvedConstants.put(name, createResolvedConstant(fieldType, value, isStatic));
				return null;
			}

			if (isStatic) {
				staticFieldsNeedingValue.put(name, fieldType);
			} else {
				nonStaticFieldsNeedingValue.put(name, fieldType);
			}

			return null;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			return switch (name) {
				case "<init>" -> {
					MethodNode ctor = new MethodNode(access, name, descriptor, signature, exceptions);
					constructors.add(ctor);
					yield ctor;
				}
				case "<clinit>" -> staticInitializer = new MethodNode(access, name, descriptor, signature, exceptions);
				default -> null;
			};
		}

		@Override
		public void visitEnd() {
			if (!staticFieldsNeedingValue.isEmpty() && staticInitializer != null) {
				Map<String, @Nullable Object> staticFieldValues = new HashMap<>();
				for (AbstractInsnNode insn : staticInitializer.instructions) {
					if (insn.getOpcode() != Opcodes.PUTSTATIC) {
						continue;
					}

					FieldInsnNode fieldInsn = (FieldInsnNode) insn;
					if (!fieldInsn.owner.equals(ownerClass)) {
						continue;
					}

					Type fieldType = staticFieldsNeedingValue.get(fieldInsn.name);
					if (fieldType == null || !fieldType.getDescriptor().equals(fieldInsn.desc)) {
						continue;
					}

					AbstractInsnNode prevInsn = AbstractInsnNodes.previousInstruction(insn);
					if (prevInsn == null || !AbstractInsnNodes.hasLiteralValue(prevInsn)) {
						// static field assigned to non-constant value
						staticFieldValues.remove(fieldInsn.name);
						staticFieldsNeedingValue.remove(fieldInsn.name);
						continue;
					}

					Object value = AbstractInsnNodes.getLiteralValue(prevInsn);
					if (staticFieldValues.containsKey(fieldInsn.name)) {
						if (!Objects.equals(staticFieldValues.get(fieldInsn.name), value)) {
							// static field assigned to multiple contradicting values
							staticFieldValues.remove(fieldInsn.name);
							staticFieldsNeedingValue.remove(fieldInsn.name);
						}
					} else {
						staticFieldValues.put(fieldInsn.name, value);
					}
				}

				staticFieldValues.forEach((name, value) -> resolvedConstants.put(name, createResolvedConstant(staticFieldsNeedingValue.get(name), value, true)));
			}

			if (!nonStaticFieldsNeedingValue.isEmpty()) {
				Map<String, @Nullable Object> nonStaticFieldValues = new HashMap<>();
				for (MethodNode constructor : constructors) {
					MethodInsnNode delegateConstructorCall = findDelegateConstructorCall(constructor.instructions);
					if (delegateConstructorCall == null) {
						// can't find this() or super() call, skip constructor
						continue;
					}

					Set<String> unassignedFields;
					if (delegateConstructorCall.owner.equals(ownerClass)) {
						// in the case of a this() call, fields need to be assigned by the delegate constructor, not this one
						unassignedFields = new HashSet<>();
					} else {
						// in the case of a super() call, any field not assigned by this constructor is not constant,
						// even if assigned by other constructors.
						unassignedFields = new HashSet<>(nonStaticFieldsNeedingValue.keySet());
					}

					for (AbstractInsnNode insn : constructor.instructions) {
						if (insn.getOpcode() != Opcodes.PUTFIELD) {
							continue;
						}

						FieldInsnNode fieldInsn = (FieldInsnNode) insn;
						if (!fieldInsn.owner.equals(ownerClass)) {
							continue;
						}

						Type fieldType = nonStaticFieldsNeedingValue.get(fieldInsn.name);
						if (fieldType == null || !fieldType.getDescriptor().equals(fieldInsn.desc)) {
							continue;
						}

						AbstractInsnNode prevInsn = AbstractInsnNodes.previousInstruction(insn);
						if (prevInsn == null || !AbstractInsnNodes.hasLiteralValue(prevInsn)) {
							// non-static field assigned to non-constant value
							nonStaticFieldValues.remove(fieldInsn.name);
							nonStaticFieldsNeedingValue.remove(fieldInsn.name);
							continue;
						}

						unassignedFields.remove(fieldInsn.name);

						Object value = AbstractInsnNodes.getLiteralValue(prevInsn);
						if (nonStaticFieldValues.containsKey(fieldInsn.name)) {
							if (!Objects.equals(nonStaticFieldValues.get(fieldInsn.name), value)) {
								// non-static field assigned to multiple contradicting values
								nonStaticFieldValues.remove(fieldInsn.name);
								nonStaticFieldsNeedingValue.remove(fieldInsn.name);
							}
						} else {
							nonStaticFieldValues.put(fieldInsn.name, value);
						}
					}

					nonStaticFieldValues.keySet().removeAll(unassignedFields);
					nonStaticFieldsNeedingValue.keySet().removeAll(unassignedFields);
				}

				nonStaticFieldValues.forEach((name, value) -> resolvedConstants.put(name, createResolvedConstant(nonStaticFieldsNeedingValue.get(name), value, false)));
			}
		}

		@Nullable
		private static MethodInsnNode findDelegateConstructorCall(InsnList instructions) {
			int newCalls = 0;
			for (AbstractInsnNode insn : instructions) {
				switch (insn.getOpcode()) {
					case Opcodes.NEW -> newCalls++;
					case Opcodes.INVOKESPECIAL -> {
						MethodInsnNode methodInsn = (MethodInsnNode) insn;
						if ("<init>".equals(methodInsn.name)) {
							if (newCalls == 0) {
								return methodInsn;
							} else {
								newCalls--;
							}
						}
					}
				}
			}

			return null;
		}

		private static ResolvedConstant createResolvedConstant(Type type, @Nullable Object value, boolean isStatic) {
			// downcast value to the field type for the narrow types that don't have bytecode literals
			switch (type.getSort()) {
				case Type.BYTE -> {
					if (value instanceof Number number) {
						value = number.byteValue();
					}
				}
				case Type.SHORT -> {
					if (value instanceof Number number) {
						value = number.shortValue();
					}
				}
				case Type.CHAR -> {
					if (value instanceof Number number) {
						value = (char) number.intValue();
					}
				}
			}
			return new ResolvedConstant(type, value, isStatic);
		}
	}
}

package daomephsta.unpick.impl.constantresolvers;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Resolves constants by analysing the bytecode of their owners.
 * @author Daomephsta
 */
public class BytecodeAnalysisConstantResolver implements IConstantResolver
{
	static final Set<Type> VALID_CONSTANT_TYPES = new HashSet<>();
	static
	{
		VALID_CONSTANT_TYPES.add(Type.BYTE_TYPE);
		VALID_CONSTANT_TYPES.add(Type.SHORT_TYPE);
		VALID_CONSTANT_TYPES.add(Type.CHAR_TYPE);
		VALID_CONSTANT_TYPES.add(Type.INT_TYPE);
		VALID_CONSTANT_TYPES.add(Type.LONG_TYPE);
		VALID_CONSTANT_TYPES.add(Type.FLOAT_TYPE);
		VALID_CONSTANT_TYPES.add(Type.DOUBLE_TYPE);
		VALID_CONSTANT_TYPES.add(Type.getObjectType("java/lang/String"));
		VALID_CONSTANT_TYPES.add(Type.getObjectType("java/lang/Class"));
	}

	private final ConcurrentMap<String, ResolvedConstants> constantDataCache = new ConcurrentHashMap<>();
	private final IClassResolver classResolver;

	public BytecodeAnalysisConstantResolver(IClassResolver classResolver)
	{
		this.classResolver = classResolver;
	}

	@Override
	public ResolvedConstant resolveConstant(String owner, String name)
	{
		ResolvedConstants resolvedConstants = constantDataCache.computeIfAbsent(owner, this::extractConstants);
		return resolvedConstants == null ? null : resolvedConstants.get(name);
	}

	private ResolvedConstants extractConstants(String owner)
	{
		ClassReader cr = classResolver.resolveClass(owner);
		if (cr == null)
			return null;
		ResolvedConstants resolvedConstants = new ResolvedConstants(Opcodes.ASM9);
		cr.accept(resolvedConstants, 0);
		return resolvedConstants;
	}

	private static class ResolvedConstants extends ClassVisitor
	{
		public ResolvedConstants(int api)
		{
			super(api);
		}

		private final Map<String, ResolvedConstant> resolvedConstants = new HashMap<>();

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value)
		{
			if (Modifier.isStatic(access) && Modifier.isFinal(access))
			{
				Type fieldType = Type.getType(descriptor);
				if (VALID_CONSTANT_TYPES.contains(fieldType))
					resolvedConstants.put(name, new ResolvedConstant(fieldType, value));
			}
			return super.visitField(access, name, descriptor, signature, value);
		}

		public ResolvedConstant get(String key)
		{
			return resolvedConstants.get(key);
		}
	}
}

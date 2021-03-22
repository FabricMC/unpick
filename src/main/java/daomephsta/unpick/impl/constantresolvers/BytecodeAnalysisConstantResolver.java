package daomephsta.unpick.impl.constantresolvers;

import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Modifier;
import java.util.*;

import org.objectweb.asm.*;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.impl.LiteralType;

/**
 * Resolves constants by analysing the bytecode of their owners.
 * @author Daomephsta
 */
public class BytecodeAnalysisConstantResolver implements IConstantResolver
{
	private static final Set<Type> VALID_CONSTANT_TYPES = Arrays.stream(LiteralType.values()).map(LiteralType::getType).collect(toSet());
	
	private final Map<String, ResolvedConstants> constantDataCache = new HashMap<>();
	private final IClassResolver classResolver;
	
	public BytecodeAnalysisConstantResolver(IClassResolver classResolver)
	{
		this.classResolver = classResolver;
	}

	@Override
	public ResolvedConstant resolveConstant(String owner, String name)
	{
		return constantDataCache.computeIfAbsent(owner, this::extractConstants).get(name);
	}

	private ResolvedConstants extractConstants(String owner)
	{
		ClassReader cr = classResolver.resolveClass(owner);
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
				if (VALID_CONSTANT_TYPES.stream().anyMatch(t -> t.equals(fieldType)))
					resolvedConstants.put(name, new ResolvedConstant(fieldType, value));
			}
			return super.visitField(access, name, descriptor, signature, value);
		}
		
		public ResolvedConstant get(Object key)
		{
			return resolvedConstants.get(key);
		}
	}
}

package daomephsta.unpick.impl.representations;

import java.util.*;

import org.objectweb.asm.*;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.IClassResolver.ClassResolutionException;

public class TargetImplementations
{
	private final Map<String, TargetInvocation> methods;
	private final IClassResolver classResolver;

	private TargetImplementations(IClassResolver classResolver, Map<String, TargetInvocation> methods)
	{
		this.classResolver = classResolver;
		this.methods = methods;
	}

	public boolean targets(String methodOwner, String methodName, String methodDescriptor)
	{
		TargetInvocation targetMethod = methods.get(methodName + methodDescriptor);
		if (targetMethod == null)
			return false;
		return targetMethod.implementedBy(classResolver, methodOwner);	
	}

	public boolean targets(String methodOwner, String methodName, String methodDescriptor, int parameterIndex)
	{
		TargetInvocation targetMethod = methods.get(methodName + methodDescriptor);
		return targetMethod.hasParameterConstantGroup(parameterIndex);
	}

	public String getParameterConstantGroup(String methodOwner, String methodName, String methodDescriptor, int parameterIndex)
	{
		TargetInvocation targetMethod = methods.get(methodName + methodDescriptor);
		return targetMethod.getParameterConstantGroup(parameterIndex);
	}
	
	public static class Builder
	{
		private final IClassResolver classResolver;
		private final Map<String, TargetInvocation> targetMethods = new HashMap<>();

		public Builder(IClassResolver classResolver)
		{
			this.classResolver = classResolver;
		}

		public void putInvocation(String owner, String name, Type descriptor, Map<Integer, String> parameterConstantGroups)
		{
			targetMethods.put(name + descriptor, new TargetInvocation(owner, name, descriptor, parameterConstantGroups));
		}

		public TargetImplementations build()
		{
			return new TargetImplementations(classResolver, targetMethods);
		}
	}
	
	/**
	 * Represents a method with parameters that may be inlined constants
	 * @author Daomephsta
	 */
	public static class TargetInvocation
	{
		private final String declarator,
							 name;
		private final Set<String> implementors = new HashSet<>(),
								  nonimplementors = new HashSet<>();
		private final Type descriptor;
		private final Map<Integer, String> parameterConstantGroups;
		
		/**
		 * Constructs a new instance of TargetMethod with the specified parameters.
		 * @param owner the internal name of the class that owns the represented method.
		 * @param name the name of the represented method
		 * @param descriptor the descriptor of the represented method.
		 * @param parameterConstantGroups a Map that maps a parameter index to the name
		 * of the constant group that contains all valid constants for that parameter. 
		 */
		public TargetInvocation(String owner, String name, Type descriptor, Map<Integer, String> parameterConstantGroups)
		{
			this.declarator = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.parameterConstantGroups = parameterConstantGroups;
		}
		
		/**
		 * @param parameterIndex the index of the parameter.
		 * @return the name of the constant group that contains all valid constants 
		 * for the parameter with an index of {@code parameterIndex}
		 */
		public String getParameterConstantGroup(int parameterIndex)
		{
			return parameterConstantGroups.get(parameterIndex);
		}
		
		/**
		 * @param parameterIndex the index of the parameter.
		 * @return true if a constant group mapping exists for the parameter 
		 * with an index of {@code parameterIndex}
		 */ 
		public boolean hasParameterConstantGroup(int parameterIndex)
		{
			return parameterConstantGroups.containsKey(parameterIndex);
		}
		
		public boolean implementedBy(IClassResolver classResolver, String classInternalName)
		{
			if (declarator.equals(classInternalName) || implementors.contains(classInternalName))
				return true;
			if (nonimplementors.contains(classInternalName))
				return false;
			if (InheritanceChecker.inheritsFrom(Opcodes.ASM9, classResolver, classInternalName, this.declarator))
			{
				implementors.add(classInternalName);
				return true;
			}
			else
			{
				nonimplementors.add(classInternalName);
				return false;
			}
		}

		@Override
		public String toString()
		{
			return String.format("TargetMethod {Qualified Name: %s.%s, Descriptor: %s, Parameter Constant Groups: %s}", 
				declarator, name, descriptor, parameterConstantGroups);
		}
	}
	
	private static class InheritanceChecker extends ClassVisitor
	{
		private final IClassResolver classResolver;
		private final String targetOwner;
		private boolean result = false;
		
		public static boolean inheritsFrom(int api, IClassResolver classResolver, String clazz, String targetOwner)
		{ 
			try
			{
				ClassReader classReader = classResolver.resolveClassReader(clazz);
				InheritanceChecker inheritanceChecker 
					= new InheritanceChecker(api, classResolver, targetOwner);
				classReader.accept(inheritanceChecker, 0); 
				return inheritanceChecker.result;
			}
			catch (ClassResolutionException e)
			{
				e.printStackTrace();
				return false;
			}
		}

		public InheritanceChecker(int api, IClassResolver classResolver, String targetOwner)
		{
			super(api);
			this.classResolver = classResolver;
			this.targetOwner = targetOwner;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
		{
			if (targetOwner.equals(name))
			{
				result = true;
				return;
			}
			try
			{
				if (superName != null && !superName.equals("java/lang/Object"))
				{
					ClassReader classReader = classResolver.resolveClassReader(superName);
					classReader.accept(this, 0);
					if (result) return;
				}
				if (interfaces != null)
				{
					for (String iface : interfaces)
					{
						ClassReader classReader = classResolver.resolveClassReader(iface);
						classReader.accept(this, 0);
						if (result) return;
					}
				}
			}
			catch (ClassResolutionException e)
			{
				e.printStackTrace();
			}
		}
	}
}

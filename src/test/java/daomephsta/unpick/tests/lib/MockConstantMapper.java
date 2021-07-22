package daomephsta.unpick.tests.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.objectweb.asm.Type;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.impl.constantmappers.SimpleAbstractConstantMapper;
import daomephsta.unpick.impl.representations.AbstractConstantDefinition;
import daomephsta.unpick.impl.representations.AbstractConstantGroup;
import daomephsta.unpick.impl.representations.FlagConstantGroup;
import daomephsta.unpick.impl.representations.FlagDefinition;
import daomephsta.unpick.impl.representations.SimpleConstantDefinition;
import daomephsta.unpick.impl.representations.SimpleConstantGroup;
import daomephsta.unpick.impl.representations.TargetMethods;

public class MockConstantMapper extends SimpleAbstractConstantMapper
{
	private final TargetMethods targetInvocations;

	private MockConstantMapper(Map<String, AbstractConstantGroup<?>> constantGroups, IConstantResolver constantResolver, TargetMethods targetInvocations)
	{
		super(constantGroups);
		this.targetInvocations = targetInvocations;
        boolean resolved = true;
        for (AbstractConstantGroup<?> group : constantGroups.values())
        {
            group.resolveAllConstants(constantResolver);
            if (!group.isResolved())
                resolved = false;
        }
        if (!resolved)
            throw new RuntimeException("One or more constants failed to resolve, check the log for details");
	}

	public static Builder builder(IClassResolver classResolver, IConstantResolver constantResolver)
	{
		return new Builder(classResolver, constantResolver);
	}

	@Override
	protected TargetMethods getTargetMethods()
	{
		return targetInvocations;
	}
	
	public static class Builder
	{
		private final Map<String, AbstractConstantGroup<?>> constantGroups = new HashMap<>();
		private final TargetMethods.Builder targetMethodsBuilder;
        private final IConstantResolver constantResolver;
		
		public Builder(IClassResolver classResolver, IConstantResolver constantResolver)
		{
			this.targetMethodsBuilder = TargetMethods.builder(classResolver);
			this.constantResolver = constantResolver;
		}

		public TargetMethodBuilder targetMethod(Class<?> owner, String name, String descriptor)
		{
			return targetMethod(owner.getName().replace('.', '/'), name, descriptor);
		}

		public TargetMethodBuilder targetMethod(String owner, String name, String descriptor)
		{
			return new TargetMethodBuilder(this, owner, name, descriptor);
		}
		
		public ConstantGroupBuilder<SimpleConstantDefinition> simpleConstantGroup(String name)
		{
			return new ConstantGroupBuilder<>(this, name, SimpleConstantDefinition::new, SimpleConstantGroup::new);
		}
		
		public ConstantGroupBuilder<FlagDefinition> flagConstantGroup(String name)
		{
			return new ConstantGroupBuilder<>(this, name, FlagDefinition::new, FlagConstantGroup::new);
		}
		
		public MockConstantMapper build()
		{
			return new MockConstantMapper(constantGroups, constantResolver, targetMethodsBuilder.build());
		}
	}
	
	public static abstract class ChildBuilder
	{
		protected final Builder parent;

		ChildBuilder(Builder parent)
		{
			this.parent = parent;
		}
	}
	
	public static class TargetMethodBuilder extends ChildBuilder
	{	
		private final TargetMethods.TargetMethodBuilder targetMethodBuilder;

		TargetMethodBuilder(Builder parent, String owner, String name, String descriptor)
		{
			super(parent);
			this.targetMethodBuilder = parent.targetMethodsBuilder.targetMethod(owner, name, Type.getType(descriptor));
		}
		
		public TargetMethodBuilder remapParameter(int parameterIndex, String constantGroup)
		{
			targetMethodBuilder.parameterGroup(parameterIndex, constantGroup);
			return this;
		}
		
		public TargetMethodBuilder remapReturn(String constantGroup)
		{
			targetMethodBuilder.returnGroup(constantGroup);
			return this;
		}
		
		public Builder add()
		{
			targetMethodBuilder.add();
			return parent;
		}
	}
	
	public static class ConstantGroupBuilder<T extends AbstractConstantDefinition<T>> extends ChildBuilder
	{
		private final String name;
		private final Collection<T> constantDefinitions = new ArrayList<>();
		private final BiFunction<String, String, T> definitionFactory;
		private final Function<String, AbstractConstantGroup<T>> groupFactory;
		
		ConstantGroupBuilder(Builder parent, String name,
				BiFunction<String, String, T> definitionFactory,
				Function<String, AbstractConstantGroup<T>> groupFactory)
		{
			super(parent);
			this.name = name;
			this.definitionFactory = definitionFactory;
			this.groupFactory = groupFactory;
		}

		public ConstantGroupBuilder<T> define(Class<?> owner, String name)
		{
			return define(owner.getName(), name);
		}
		
		public ConstantGroupBuilder<T> define(String owner, String name)
		{
			constantDefinitions.add(definitionFactory.apply(owner.replace('.', '/'), name));
			return this;
		}
		
		public ConstantGroupBuilder<T> defineAll(Class<?> owner, String... names)
		{
			for (String name : names)
			{
				define(owner, name);
			}
			return this;
		}
		
		public Builder add()
		{
			AbstractConstantGroup<T> group = groupFactory.apply(name);
			for (T constant : constantDefinitions)
			{
				group.add(constant);
			}
			if (parent.constantGroups.putIfAbsent(name, group) != null)
				throw new IllegalStateException("A constant group named " + name + " already exists");
			return parent;
		}
	}
}

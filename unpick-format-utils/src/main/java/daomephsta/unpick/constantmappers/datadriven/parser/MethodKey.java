package daomephsta.unpick.constantmappers.datadriven.parser;

import java.util.Objects;

/**
 * Immutable data object that encapsulates a method owner, name and descriptor
 * so that they can be used as a map key.
 * @author Daomephsta
 *
 * @deprecated Use {@link MemberKey} instead.
 */
@Deprecated
public class MethodKey
{
	private final String owner, name, descriptor;

	public MethodKey(String owner, String name, String descriptor)
	{
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
	}

	public MemberKey toMemberKey()
	{
		return new MemberKey(owner, name, descriptor);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(descriptor, name, owner);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodKey other = (MethodKey) obj;
		return Objects.equals(descriptor, other.descriptor)
				&& Objects.equals(name, other.name)
				&& Objects.equals(owner, other.owner);
	}

	@Override
	public String toString()
	{
		return String.format("MethodKey (%s.%s descriptor=%s)", owner, name, descriptor);
	}
}
package daomephsta.unpick.constantmappers.datadriven.parser;

import java.util.Objects;

/**
 * Immutable data object that encapsulates a field owner and name
 * so that they can be used as a map key.
 *
 * @deprecated Use {@link MemberKey} instead.
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public class FieldKey
{
	final String owner, name;

	public FieldKey(String owner, String name)
	{
		this.owner = owner;
		this.name = name;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FieldKey fieldKey = (FieldKey) o;
		return owner.equals(fieldKey.owner) && name.equals(fieldKey.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(owner, name);
	}

	@Override
	public String toString()
	{
		return String.format("FieldKey (%s.%s)", owner, name);
	}
}

package daomephsta.unpick.constantmappers.datadriven.parser;

import java.util.Objects;

/**
 * Immutable data object that encapsulates a member's owner, name and descriptor
 * so that they can be used as a map key.
 * @author Daomephsta
 */
public final class MemberKey {
	private final String owner;
	private final String name;
	private final String descriptor;

	public MemberKey(String owner, String name, String descriptor) {
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
	}

	@Override
	public int hashCode() {
		return Objects.hash(descriptor, name, owner);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MemberKey other = (MemberKey) obj;
		return Objects.equals(descriptor, other.descriptor)
				&& Objects.equals(name, other.name)
				&& Objects.equals(owner, other.owner);
	}

	@Override
	public String toString() {
		return String.format("MemberKey (%s.%s descriptor=%s)", owner, name, descriptor);
	}
}

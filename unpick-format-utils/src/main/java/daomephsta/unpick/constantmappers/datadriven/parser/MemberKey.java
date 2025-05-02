package daomephsta.unpick.constantmappers.datadriven.parser;

/**
 * Immutable data object that encapsulates a member's owner, name and descriptor
 * so that they can be used as a map key.
 *
 * @author Daomephsta
 */
public record MemberKey(String owner, String name, String descriptor) {
}

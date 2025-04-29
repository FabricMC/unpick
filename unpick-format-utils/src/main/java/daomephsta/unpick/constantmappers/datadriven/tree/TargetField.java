package daomephsta.unpick.constantmappers.datadriven.tree;

public final class TargetField {
	public final String className;
	public final String fieldName;
	public final String fieldDesc;
	public final String groupName;

	public TargetField(String className, String fieldName, String fieldDesc, String groupName) {
		this.className = className;
		this.fieldName = fieldName;
		this.fieldDesc = fieldDesc;
		this.groupName = groupName;
	}
}

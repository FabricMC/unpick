package daomephsta.unpick.impl.constantmappers.datadriven.data;

import java.util.HashMap;
import java.util.Map;

import daomephsta.unpick.constantmappers.datadriven.parser.MemberKey;
import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

public final class GroupInfo {
	public final DataType dataType;
	public final boolean flags;
	public final ScopedGroupInfo globalScope = new ScopedGroupInfo();
	public final Map<String, ScopedGroupInfo> packageScopes = new HashMap<>();
	public final Map<String, ScopedGroupInfo> classScopes = new HashMap<>();
	public final Map<MemberKey, ScopedGroupInfo> methodScopes = new HashMap<>();

	public GroupInfo(DataType dataType, boolean flags) {
		this.dataType = dataType;
		this.flags = flags;
	}
}

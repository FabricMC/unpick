package daomephsta.unpick.impl.constantmappers.datadriven.data;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import daomephsta.unpick.constantmappers.datadriven.tree.GroupFormat;

public final class ScopedGroupInfo {
	@Nullable
	public GroupFormat format;
	public final Map<Object, ConstantReplacementInfo> constantReplacementMap = new HashMap<>();
}

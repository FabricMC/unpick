package daomephsta.unpick.api.constantgroupers;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantGrouper;

import java.io.InputStream;

public final class ConstantGroupers
{
	private ConstantGroupers()
	{
	}

	public static IConstantGrouper dataDriven(IConstantResolver constantResolver, InputStream... mappingSources)
	{
		return new DataDrivenConstantGrouper(constantResolver, mappingSources);
	}
}

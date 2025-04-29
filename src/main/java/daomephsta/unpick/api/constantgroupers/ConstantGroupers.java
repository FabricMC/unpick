package daomephsta.unpick.api.constantgroupers;

import java.io.InputStream;

import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantGrouper;

public final class ConstantGroupers {
	private ConstantGroupers() {
	}

	public static IConstantGrouper dataDriven(IInheritanceChecker inheritanceChecker, InputStream... mappingSources) {
		return new DataDrivenConstantGrouper(null, inheritanceChecker, mappingSources);
	}

	public static IConstantGrouper dataDriven(IConstantResolver constantResolver, IInheritanceChecker inheritanceChecker, InputStream... mappingSources) {
		return new DataDrivenConstantGrouper(constantResolver, inheritanceChecker, mappingSources);
	}
}

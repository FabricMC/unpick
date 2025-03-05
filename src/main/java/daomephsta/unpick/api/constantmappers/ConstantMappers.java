package daomephsta.unpick.api.constantmappers;

import java.io.InputStream;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.constantgroupers.ConstantGroupers;
import daomephsta.unpick.api.constantresolvers.ConstantResolvers;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantGrouper;

/**
 * API methods for creating instances of predefined implementations of {@link IConstantMapper}
 * @author Daomephsta
 *
 * @deprecated Use {@link ConstantGroupers} instead.
 */
@Deprecated
public class ConstantMappers
{
	/**
	 * @return a constant mapper that uses the mappings defined by {@code mappingSources}
	 * @param classResolver a class resolver that can resolve the classes of the target methods
	 * @param mappingSources streams of text in <a href="https://github.com/Daomephsta/unpick/wiki/Unpick-Format">.unpick format</a>
	 * @throws UnpickSyntaxException if any of the mapping sources have invalid syntax
	 */
	public static IConstantMapper dataDriven(IClassResolver classResolver, InputStream... mappingSources)
	{
		return new DataDrivenConstantGrouper(ConstantResolvers.bytecodeAnalysis(classResolver), mappingSources).asMapper();
	}
}

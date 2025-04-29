package daomephsta.unpick.api.constantmappers;

import java.io.InputStream;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantMapper;

/**
 * API methods for creating instances of predefined implementations of {@link IConstantMapper}.
 * @author Daomephsta
 */
public class ConstantMappers {
	/**
	 * @param classResolver a class resolver that can resolve the classes of the target methods
	 * @param mappingSources streams of text in <a href="https://github.com/Daomephsta/unpick/wiki/Unpick-Format">.unpick format</a>
	 * @return a constant mapper that uses the mappings defined by {@code mappingSources}
	 * @throws UnpickSyntaxException if any of the mapping sources have invalid syntax
	 */
	public static IConstantMapper dataDriven(IClassResolver classResolver, InputStream... mappingSources) {
		return new DataDrivenConstantMapper(classResolver, mappingSources);
	}
}

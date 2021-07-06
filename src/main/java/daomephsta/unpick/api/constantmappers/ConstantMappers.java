package daomephsta.unpick.api.constantmappers;

import java.io.InputStream;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantMapper;

/**
 * API methods for creating instances of predefined implementations of {@link IConstantMapper}
 * @author Daomephsta
 */
public class ConstantMappers
{	
	/**
	 * Creates a data-driven constant mapper. Constants are resolved immediately.
     * @return a constant mapper that uses the mappings defined by {@code mappingSources}
     * @param classResolver a class resolver that can resolve the classes of the target methods
     * @param constantResolver a constant resolver that can resolve the target constant fields
     * @param mappingSources streams of text in <a href="https://github.com/Daomephsta/unpick/wiki/Unpick-Format">.unpick format</a>
     * @throws UnpickSyntaxException if any of the mapping sources have invalid syntax
     */
    public static IConstantMapper dataDriven(IClassResolver classResolver, IConstantResolver constantResolver, InputStream... mappingSources)
    {
        return new DataDrivenConstantMapper(classResolver, constantResolver, mappingSources);
    }
}

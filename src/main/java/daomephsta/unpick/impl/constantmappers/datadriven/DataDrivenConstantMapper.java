package daomephsta.unpick.impl.constantmappers.datadriven;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Logger;

import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.impl.constantmappers.SimpleAbstractConstantMapper;
import daomephsta.unpick.impl.constantmappers.datadriven.parser.V1Parser;
import daomephsta.unpick.impl.constantmappers.datadriven.parser.v2.V2Parser;
import daomephsta.unpick.impl.representations.TargetMethods;

/**
 * Maps inlined values to constants using mappings defined in a file.
 * @author Daomephsta
 */
public class DataDrivenConstantMapper extends SimpleAbstractConstantMapper {
	private static final Logger LOGGER = Logger.getLogger("unpick");
	private final TargetMethods targetMethods;

	public DataDrivenConstantMapper(IClassResolver classResolver, InputStream... mappingSources) {
		super(new HashMap<>());
		TargetMethods.Builder targetMethodsBuilder = TargetMethods.builder(classResolver);
		for (InputStream mappingSource : mappingSources) {
			try {
				//Avoid buffering, so that only the version specifier bytes are consumed
				byte[] version = new byte[2];
				mappingSource.read(version);
				if (version[0] == 'v') {
					switch (version[1]) {
						case '1':
							V1Parser.INSTANCE.parse(mappingSource, constantGroups, targetMethodsBuilder);
							break;

						case '2':
							V2Parser.parse(mappingSource, constantGroups, targetMethodsBuilder);
							break;

						default :
							throw new UnpickSyntaxException(1, "Unknown version " + (char) version[1]);
					}
				} else {
					throw new UnpickSyntaxException(1, "Missing version");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.targetMethods = targetMethodsBuilder.build();
		LOGGER.info("Loaded " + targetMethods);
	}

	@Override
	protected TargetMethods getTargetMethods() {
		return targetMethods;
	}
}

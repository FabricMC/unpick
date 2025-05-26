package daomephsta.unpick.api.constantgroupers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Logger;

import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.classresolvers.IConstantResolver;
import daomephsta.unpick.api.classresolvers.IInheritanceChecker;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantGrouper;

public final class ConstantGroupers {
	private ConstantGroupers() {
	}

	public static DataDrivenBuilder dataDriven() {
		return new DataDrivenBuilder();
	}

	public static final class DataDrivenBuilder {
		private Logger logger;
		private boolean lenient = false;
		private IConstantResolver constantResolver;
		private IInheritanceChecker inheritanceChecker;
		private String methodWhichInitializedResult;
		private DataDrivenConstantGrouper result;

		private DataDrivenBuilder() {
		}

		public DataDrivenBuilder logger(Logger logger) {
			ensureGrouperNotInitialized("logger");
			this.logger = logger;
			return this;
		}

		public DataDrivenBuilder lenient(boolean lenient) {
			ensureGrouperNotInitialized("lenient");
			this.lenient = lenient;
			return this;
		}

		public DataDrivenBuilder classResolver(IClassResolver classResolver) {
			ensureGrouperNotInitialized("classResolver");
			this.constantResolver = classResolver.asConstantResolver();
			this.inheritanceChecker = classResolver.asInheritanceChecker();
			return this;
		}

		public DataDrivenBuilder constantResolver(IConstantResolver constantResolver) {
			ensureGrouperNotInitialized("constantResolver");
			this.constantResolver = constantResolver;
			return this;
		}

		public DataDrivenBuilder inheritanceChecker(IInheritanceChecker inheritanceChecker) {
			ensureGrouperNotInitialized("inheritanceChecker");
			this.inheritanceChecker = inheritanceChecker;
			return this;
		}

		public DataDrivenBuilder mappingSource(InputStream mappingSource) throws IOException {
			return mappingSource(new InputStreamReader(mappingSource));
		}

		public DataDrivenBuilder mappingSource(Reader mappingSource) throws IOException {
			ensureGrouperInitialized("mappingSource");
			result.loadData(mappingSource);
			return this;
		}

		public IConstantGrouper build() {
			ensureGrouperInitialized("build");
			logger.info(() -> String.format("Loaded %d constant groups, %d target fields and %d target methods", result.groupCount(), result.targetFieldCount(), result.targetMethodCount()));
			return result;
		}

		private void ensureGrouperNotInitialized(String methodName) {
			if (result != null) {
				throw new IllegalStateException("Cannot call " + methodName + " after " + methodWhichInitializedResult);
			}
		}

		private void ensureGrouperInitialized(String methodName) {
			if (constantResolver == null) {
				throw new IllegalStateException("Cannot call " + methodName + " without setting the constant resolver");
			}
			if (inheritanceChecker == null) {
				throw new IllegalStateException("Cannot call " + methodName + " without setting the inheritance checker");
			}
			if (logger == null) {
				logger = Logger.getLogger("unpick");
			}
			if (result == null) {
				methodWhichInitializedResult = methodName;
				result = new DataDrivenConstantGrouper(logger, lenient, constantResolver, inheritanceChecker);
			}
		}
	}
}

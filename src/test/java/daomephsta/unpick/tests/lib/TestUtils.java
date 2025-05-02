package daomephsta.unpick.tests.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.classresolvers.ClassResolvers;
import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantGrouper;

public class TestUtils {
	private static final Path TEST_DATA = Paths.get(System.getProperty("testData"));
	private static final Path TEST_DATA_EXPECTED = Paths.get(System.getProperty("testDataExpected"));

	public static void runTest(String className, Consumer<UnpickV3Visitor> dataProvider) {
		ClassNode clazz = readClass(TEST_DATA, className);
		ClassNode expectedClass = readClass(TEST_DATA_EXPECTED, className);

		IClassResolver classResolver = ClassResolvers.fromPath(TEST_DATA).chain(ClassResolvers.classpath());
		ConstantUninliner.builder()
				.grouper(new DataDrivenConstantGrouper(classResolver.asConstantResolver(), classResolver.asInheritanceChecker(), dataProvider))
				.classResolver(classResolver)
				.build()
				.transform(clazz);

		ASMAssertions.assertClassEquals(expectedClass, clazz);
	}

	private static ClassNode readClass(Path testDataDir, String className) {
		try (InputStream in = Files.newInputStream(testDataDir.resolve(className + ".class"))) {
			ClassNode node = new ClassNode();
			ClassReader reader = new ClassReader(in);
			reader.accept(node, 0);
			return node;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

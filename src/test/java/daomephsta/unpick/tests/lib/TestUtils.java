package daomephsta.unpick.tests.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.classresolvers.ClassResolvers;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantGrouper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class TestUtils
{
	private static final Path TEST_DATA = Paths.get(System.getProperty("testData"));
	private static final Path TEST_DATA_EXPECTED = Paths.get(System.getProperty("testDataExpected"));

	public static void runTest(String className, Consumer<UnpickV3Visitor> dataProvider)
	{
		ClassNode clazz = readClass(TEST_DATA, className);
		ClassNode expectedClass = readClass(TEST_DATA_EXPECTED, className);

		ConstantUninliner.builder()
			.grouper(new DataDrivenConstantGrouper(dataProvider))
			.classResolver(ClassResolvers.fromPath(TEST_DATA).chain(ClassResolvers.classpath()))
			.build()
			.transform(clazz);

		ASMAssertions.assertClassEquals(expectedClass, clazz);
	}

	private static ClassNode readClass(Path testDataDir, String className)
	{
		try (InputStream in = Files.newInputStream(testDataDir.resolve(className + ".class")))
		{
			ClassNode node = new ClassNode();
			ClassReader reader = new ClassReader(in);
			reader.accept(node, 0);
			return node;
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
}

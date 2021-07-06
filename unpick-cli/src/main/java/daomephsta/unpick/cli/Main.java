package daomephsta.unpick.cli;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.IClassResolver;
import daomephsta.unpick.api.constantmappers.ConstantMappers;
import daomephsta.unpick.api.constantresolvers.ConstantResolvers;
import daomephsta.unpick.api.constantresolvers.IConstantResolver;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("<inputJar> <outputJar> <unpickDefinition> <constantJar> [classpath...]");
            System.exit(1);
        }

        Path inputJar = Paths.get(args[0]);
        Path outputJar = Paths.get(args[1]);
        Path unpickDefinition = Paths.get(args[2]);
        Path constantJar = Paths.get(args[3]);

        Collection<Path> classpath = new LinkedList<>();

        classpath.add(constantJar);
        classpath.add(inputJar);

        for (int i = 4; i < args.length; i++) {
            classpath.add(Paths.get(args[i]));
        }

        try {
            unpick(inputJar, outputJar, unpickDefinition, classpath);
        } catch (IOException e) {
            Files.delete(outputJar);
            throw e;
        }
    }

    private static void unpick(Path inputJar, Path outputJar, Path unpickDefinition, Collection<Path> classpath) throws IOException {
        Files.deleteIfExists(outputJar);

        try (
             JarClassResolver classResolver = new JarClassResolver(classpath);
             InputStream unpickDefinitionStream = Files.newInputStream(unpickDefinition)
        ) {
            IConstantResolver constantResolver = ConstantResolvers.bytecodeAnalysis(classResolver);
            ConstantUninliner uninliner = new ConstantUninliner(
                    ConstantMappers.dataDriven(classResolver, constantResolver, unpickDefinitionStream),
                    constantResolver
            );

            try (JarFile jarFile = new JarFile(inputJar.toFile()); JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(outputJar))) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    JarEntry outputEntry = new JarEntry(entry.getName());
                    outputStream.putNextEntry(outputEntry);

                    InputStream inputStream = jarFile.getInputStream(entry);

                    if (entry.getName().endsWith(".class")) {
                        ClassReader classReader = new ClassReader(inputStream);
                        ClassNode classNode = new ClassNode();
                        classReader.accept(classNode, 0);

                        uninliner.transform(classNode);

                        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        classNode.accept(classWriter);
                        outputStream.write(classWriter.toByteArray());
                    } else {
                        byte[] buf = new byte[1024];
                        int length;

                        while ((length = inputStream.read(buf)) > 0) {
                            outputStream.write(buf, 0, length);
                        }
                    }

                    outputStream.closeEntry();
                }
            }
        }
    }

    private static class JarClassResolver implements IClassResolver, Closeable {
        private final URLClassLoader classLoader;

        public JarClassResolver(URL[] urls) {
            this.classLoader = new URLClassLoader(urls);
        }

        public JarClassResolver(Collection<Path> paths) {
            this(paths.stream().map(path -> {
                if (!Files.exists(path)) {
                    throw new RuntimeException("Could not find " + path.toString());
                }

                try {
                    return new URL(null, path.toUri().toString());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new)
            );
        }

        @Override
        public ClassReader resolveClass(String internalName) throws ClassResolutionException {
            InputStream inputStream = classLoader.getResourceAsStream(internalName + ".class");

            if (inputStream != null) {
                try {
                    return new ClassReader(inputStream);
                } catch (IOException e) {
                    throw new ClassResolutionException(e);
                }
            }

            throw new ClassResolutionException("Failed to find " + internalName);
        }

        @Override
        public void close() throws IOException {
            classLoader.close();
        }
    }
}

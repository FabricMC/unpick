package daomephsta.unpick.tests.lib;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;

import java.io.PrintWriter;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

public class MethodMocker implements Opcodes {
	public static final String CLASS_NAME = "MockClass";
	private static final String METHOD_NAME = "mock";
	private static final String[] NO_EXCEPTIONS = null;
	private static final String[] NO_INTERFACES = null;
	private static final String NO_SIGNATURE = null;
	private static final String OBJECT_SUPERCLASS = "java/lang/Object";

	public static class MockMethod {
		private final MethodNode mockMethod;
		private final ClassNode mockClass;

		public MockMethod(MethodNode mockMethod, ClassNode mockClass) {
			this.mockMethod = mockMethod;
			this.mockClass = mockClass;
		}

		public MethodNode getMockMethod() {
			return mockMethod;
		}

		public ClassNode getMockClass() {
			return mockClass;
		}
	}

	public static MockMethod mock(Class<?> returnType, Consumer<MethodVisitor> bodyGenerator) {
		ClassWriter mockClassWriter = new ClassWriter(COMPUTE_FRAMES);

		StringBuilder descriptorBuilder = new StringBuilder("()");
		descriptorBuilder.append(Type.getDescriptor(returnType));
		String descriptor = descriptorBuilder.toString();

		mockClassWriter.visit(V1_8, ACC_PUBLIC, CLASS_NAME, NO_SIGNATURE, OBJECT_SUPERCLASS, NO_INTERFACES); {
			MethodVisitor mockWriter = mockClassWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, METHOD_NAME, descriptor, NO_SIGNATURE, NO_EXCEPTIONS);
			bodyGenerator.accept(mockWriter);
			mockWriter.visitMaxs(0, 0); //Trigger computation of stack size and local variable count
			mockWriter.visitEnd();
		}
		mockClassWriter.visitEnd();
		ClassReader classReader = new ClassReader(mockClassWriter.toByteArray());
		//Check that sane code was generated
		CheckClassAdapter.verify(classReader, false, new PrintWriter(System.out));
		ClassNode mockClass = new ClassNode();
		classReader.accept(mockClass, 0);
		return new MockMethod(MethodFetcher.fetch(mockClass, METHOD_NAME + descriptor), mockClass);
	}
}

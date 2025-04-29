package daomephsta.unpick.tests.lib;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import daomephsta.unpick.impl.InstructionFactory;
import daomephsta.unpick.impl.LiteralType;
import daomephsta.unpick.impl.Utils;
import daomephsta.unpick.tests.lib.MethodMocker.MockMethod;

public class TestUtils {
	public static void printVisitable(Consumer<MethodVisitor> visitable) {
		System.out.println(Utils.visitableToString(visitable));
	}

	public static MockMethod mockInvokeStatic(Class<?> methodOwner, String methodName, String methodDescriptor, Object constant) {
		Type expectedType = Type.getArgumentTypes(methodDescriptor)[0];
		Type actualType = LiteralType.from(constant.getClass()).getType();
		if (!expectedType.equals(actualType)) {
			throw new IllegalArgumentException(String.format("Expected constant of type %s, actual type %s",
					expectedType.getClassName(), actualType.getClassName()));
		}

		return MethodMocker.mock(void.class, mv -> {
			InstructionFactory.pushesValue(mv, constant);
			mv.visitMethodInsn(INVOKESTATIC, methodOwner.getName().replace('.', '/'), methodName, methodDescriptor, false);
			mv.visitInsn(RETURN);
		});
	}

	public static void dumpClassNode(ClassNode clazz, File dumpPath, String dumpName) {
		ClassWriter cw = new ClassWriter(0);
		clazz.accept(cw);
		dumpPath.mkdirs();
		try (OutputStream out = new FileOutputStream(new File(dumpPath, dumpName + ".class"))) {
			out.write(cw.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

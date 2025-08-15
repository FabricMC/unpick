package daomephsta.unpick.tests;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import daomephsta.unpick.api.ValidatingUnpickV3Visitor;
import daomephsta.unpick.api.classresolvers.ClassResolvers;
import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Reader;

public class TestValidation {
	@Test
	public void testValid() throws IOException {
		testValidation("""
				group int class_version
				group int access_flags
					@scope package org.objectweb.asm
					@scope class org.objectweb.asm.tree.ClassNode
					@scope method org.objectweb.asm.tree.ClassNode <init> ()V
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_field org.objectweb.asm.tree.ClassNode access I access_flags
				target_method java.util.Spliterator characteristics ()I # some random method returning an int idk
					return access_flags
				target_method org.objectweb.asm.ClassVisitor visit (IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V
					param 0 class_version
				target_method org.objectweb.asm.ClassVisitor visit (IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V
					param 1 access_flags
				""",
				null
		);
	}

	@Test
	public void testNonExistentTargetField() throws IOException {
		testValidation("""
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_field org.objectweb.asm.tree.ClassNode foo I access_flags
				""",
				"No such field: org.objectweb.asm.tree.ClassNode.foo:I");
	}

	@Test
	public void testNonExistentTargetMethod() throws IOException {
		testValidation("""
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_method org.objectweb.asm.tree.ClassNode foo ()V
					return access_flags
				""",
				"No such method: org.objectweb.asm.tree.ClassNode.foo()V");
	}

	@Test
	public void testNonUnpickableField() throws IOException {
		testValidation("""
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_field org.objectweb.asm.tree.ClassNode fields Ljava/util/List; access_flags
				""",
				"Not an unpickable data type: java.util.List");
	}

	@Test
	public void testIncompatibleField() throws IOException {
		testValidation("""
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_field org.objectweb.asm.tree.ClassNode name Ljava/lang/String; access_flags
				""",
				"Target of type String declares group access_flags of incompatible type int");
	}

	@Test
	public void testIncompatibleMethodReturn() throws IOException {
		testValidation("""
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_method org.objectweb.asm.Handle getOwner ()Ljava/lang/String;
					return access_flags
				""",
				"Target of type String declares group access_flags of incompatible type int");
	}

	@Test
	public void testIncompatibleMethodParam() throws IOException {
		testValidation("""
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_method org.objectweb.asm.Handle <init> (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V
					param 1 access_flags
				""",
				"Target of type String declares group access_flags of incompatible type int");
	}

	@Test
	public void testMethodParamOutOfBounds() throws IOException {
		testValidation("""
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_method org.objectweb.asm.Handle <init> (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V
					param 42 access_flags
				""",
				"Parameter index out of bounds: 42");
	}

	@Test
	public void testNonExistentGroup() throws IOException {
		testValidation("""
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_method org.objectweb.asm.Handle <init> (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V
					param 0 foo
				""",
				"Reference to undeclared group: foo");
	}

	@Test
	public void testNonExistentPackageScope() throws IOException {
		testValidation("""
				group int access_flags
					@scope package foo.bar
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				""",
				"Package foo.bar does not exist");
	}

	@Test
	public void testNonExistentClassScope() throws IOException {
		testValidation("""
				group int access_flags
					@scope class foo.Bar
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				""",
				"Class foo.Bar does not exist");
	}

	@Test
	public void testNonExistentMethodScope() throws IOException {
		testValidation("""
				group int access_flags
					@scope method org.objectweb.asm.tree.ClassNode foo ()V
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				""",
				"Method org.objectweb.asm.tree.ClassNode.foo()V does not exist");
	}

	@Test
	public void testDuplicateTargetField() throws IOException {
		testValidation("""
				group int class_version
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_field org.objectweb.asm.tree.ClassNode access I class_version
				target_field org.objectweb.asm.tree.ClassNode access I access_flags
				""",
				"Duplicate target field: org.objectweb.asm.tree.ClassNode.access"
		);
	}

	@Test
	public void testDuplicateTargetMethodReturn() throws IOException {
		testValidation("""
				group int class_version
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_method java.util.Spliterator characteristics ()I # some random method returning an int idk
					return class_version
				target_method java.util.Spliterator characteristics ()I
					return access_flags
				""",
				"Duplicate return group: java.util.Spliterator.characteristics()I"
		);
	}

	@Test
	public void testDuplicateTargetMethodParam() throws IOException {
		testValidation("""
				group int class_version
				group int access_flags
					org.objectweb.asm.Opcodes.ACC_PUBLIC
					org.objectweb.asm.Opcodes.ACC_ABSTRACT
				target_method org.objectweb.asm.ClassVisitor visit (IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V
					param 0 class_version
				target_method org.objectweb.asm.ClassVisitor visit (IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V
					param 0 access_flags
				""",
				"Duplicate param group: org.objectweb.asm.ClassVisitor.visit(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V 0"
		);
	}

	private static void testValidation(String fileText, @Nullable String expectedError) throws IOException {
		try {
			UnpickV3Reader reader = new UnpickV3Reader(new StringReader("unpick v3\n" + fileText));
			ValidatingUnpickV3Visitor validator = new ValidatingUnpickV3Visitor(ClassResolvers.classpath()) {
				@Override
				public boolean packageExists(String packageName) {
					return Thread.currentThread().getContextClassLoader().getDefinedPackage(packageName) != null;
				}
			};
			reader.accept(validator);
			validator.finishValidation();
		} catch (UnpickSyntaxException e) {
			if (expectedError == null) {
				throw e;
			}
			assertEquals(expectedError, e.getMessage());
			return;
		}

		if (expectedError != null) {
			assertionFailure()
					.expected(expectedError)
					.actual("<no error>")
					.buildAndThrow();
		}
	}
}

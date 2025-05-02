package daomephsta.unpick.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import daomephsta.unpick.constantmappers.datadriven.tree.DataType;

public final class Utils {
	private Utils() {
	}

	public static String visitableToString(Consumer<MethodVisitor> visitable) {
		StringWriter w = new StringWriter();
		try (PrintWriter pw = new PrintWriter(w)) {
			Printer printer = new Textifier();
			MethodVisitor tracer = new TraceMethodVisitor(printer);
			visitable.accept(tracer);
			printer.print(pw);
		}
		return w.toString();
	}

	public static boolean isIntegral(Object literal) {
		return literal instanceof Byte
				|| literal instanceof Short
				|| literal instanceof Integer
				|| literal instanceof Long;
	}

	public static boolean isFloatingPoint(Object literal) {
		return literal instanceof Float || literal instanceof Double;
	}

	public static <T> T[] prepend(T value, T[] array) {
		@SuppressWarnings("unchecked")
		T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length + 1);
		newArray[0] = value;
		System.arraycopy(array, 0, newArray, 1, array.length);
		return newArray;
	}

	public static <T> T[] concat(T[] first, T[] second) {
		@SuppressWarnings("unchecked")
		T[] newArray = (T[]) Array.newInstance(first.getClass().getComponentType(), first.length + second.length);
		System.arraycopy(first, 0, newArray, 0, first.length);
		System.arraycopy(second, 0, newArray, first.length, second.length);
		return newArray;
	}

	public static DataType asmTypeToDataType(Type type) {
		return switch (type.getSort()) {
			case Type.BYTE -> DataType.BYTE;
			case Type.SHORT -> DataType.SHORT;
			case Type.INT -> DataType.INT;
			case Type.LONG -> DataType.LONG;
			case Type.FLOAT -> DataType.FLOAT;
			case Type.DOUBLE -> DataType.DOUBLE;
			case Type.CHAR -> DataType.CHAR;
			case Type.OBJECT -> switch (type.getInternalName()) {
				case "java/lang/String" -> DataType.STRING;
				case "java/lang/Class" -> DataType.CLASS;
				default -> null;
			};
			default -> null;
		};
	}

	public static String getDescriptor(DataType dataType) {
		return switch (dataType) {
			case BYTE -> "B";
			case SHORT -> "S";
			case INT -> "I";
			case LONG -> "J";
			case FLOAT -> "F";
			case DOUBLE -> "D";
			case CHAR -> "C";
			case STRING -> "Ljava/lang/String;";
			case CLASS -> "Ljava/lang/Class;";
		};
	}
}

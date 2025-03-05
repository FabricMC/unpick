package daomephsta.unpick.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.*;

public final class Utils
{
	private Utils()
	{
	}

	public static String visitableToString(Consumer<MethodVisitor> visitable)
	{
		StringWriter w = new StringWriter();
		try(PrintWriter pw = new PrintWriter(w))
		{
			Printer printer = new Textifier();
			MethodVisitor tracer = new TraceMethodVisitor(printer);
			visitable.accept(tracer);
			printer.print(pw);
		}
		return w.toString();
	}

	public static boolean isIntegral(Object literal)
	{
		return literal instanceof Byte ||
			   literal instanceof Short ||
			   literal instanceof Integer ||
			   literal instanceof Long;
	}

	public static boolean isFloatingPoint(Object literal)
	{
		return literal instanceof Float || literal instanceof Double;
	}
}

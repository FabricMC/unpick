package daomephsta.unpick.constantmappers.datadriven.parser.v2;

import static java.util.stream.Collectors.toSet;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;

/**
 * Performs basic syntax checking on .unpick v2 format text, 
 * and allows its structure to be visited by instances of {@link Visitor}. 
 * @author Daomephsta
 */
public class UnpickV2Reader implements Closeable
{
	private static final Pattern WHITESPACE_SPLITTER = Pattern.compile("\\s");
	private final InputStream definitionsStream;
	
	private TargetMethodDefinitionVisitor lastTargetMethodVisitor = null;

	/**
	 * Creates a new reader from an input stream
	 * @param definitionsStream a stream of text in 
	 * <a href="https://github.com/Daomephsta/unpick/wiki/Unpick-Format">.unpick v2 format</a> 
	 */
	public UnpickV2Reader(InputStream definitionsStream)
	{
		this.definitionsStream = definitionsStream;
	}
	
	private static final Set<String> TARGET_METHOD_ARGS = Stream.of("param", "return").collect(toSet());
	/**
	 * Makes {@code visitor} visit the structure of the parsed file
	 * @param visitor the visitor
	 */
	public void accept(Visitor visitor)
	{
		try(LineNumberReader reader = new LineNumberReader(new InputStreamReader(definitionsStream)))
		{
			Iterator<String[]> lineTokensIter = reader.lines()
				.skip(1) //Skip version
				.map(s -> stripComment(s.trim()))
				.filter(s -> !s.isEmpty()) //Discard empty lines & lines that are empty once comments are stripped
				.map(l -> Arrays.stream(WHITESPACE_SPLITTER.split(l)).filter(s -> !s.isEmpty()).toArray(String[]::new)) //Tokenise lines
				.iterator();
			visitor.startVisit();
			while(lineTokensIter.hasNext())
			{
				String[] tokens = lineTokensIter.next();
				visitor.visitLineNumber(reader.getLineNumber());
				if (tokens[0].equals("target_method"))
				{
					visitTargetMethodDefinition(visitor, tokens, reader.getLineNumber());
				}
				else
				{
					if (lastTargetMethodVisitor != null && !TARGET_METHOD_ARGS.contains(tokens[0]))
					{
						lastTargetMethodVisitor.endVisit();
						lastTargetMethodVisitor = null;
					}
					switch (tokens[0])
					{
					case "constant":
						visitSimpleConstantDefinition(visitor, tokens, reader.getLineNumber());
						break;
					
					case "flag":
						visitFlagConstantDefinition(visitor, tokens, reader.getLineNumber());
						break;
						
					case "param":
						visitParameterConstantGroupDefinition(visitor, tokens, reader.getLineNumber());
						break;
						
					case "return":
						visitReturnConstantGroupDefinition(visitor, tokens, reader.getLineNumber());
						break;
						
					default:
						throw new UnpickSyntaxException("\nUnknown start token Tokens: " + Arrays.toString(tokens));
					}
				}
			}
			if (lastTargetMethodVisitor != null)
			{
				lastTargetMethodVisitor.endVisit();
				lastTargetMethodVisitor = null;
			}
			visitor.endVisit();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void visitParameterConstantGroupDefinition(Visitor visitor, String[] tokens, int lineNumber)
	{
		if (lastTargetMethodVisitor == null)
			throw new UnpickSyntaxException(lineNumber, "Invalid parameter constant group definition, not part of target method definition");
		if (tokens.length != 3)
			throw new UnpickSyntaxException(lineNumber, "Unexpected token count. Expected 3. Found " + tokens.length);
		try
		{
			lastTargetMethodVisitor.visitParameterGroupDefinition(Integer.parseInt(tokens[1]), tokens[2]);
		}
		catch (NumberFormatException e)
		{
			throw new UnpickSyntaxException(lineNumber, "Could not parse " + tokens[1] + " as an integer");
		}
	}
	
	private void visitReturnConstantGroupDefinition(Visitor visitor, String[] tokens, int lineNumber)
	{
		if (lastTargetMethodVisitor == null)
			throw new UnpickSyntaxException(lineNumber, "Invalid return constant group definition, not part of target method definition");
		if (tokens.length != 2)
			throw new UnpickSyntaxException(lineNumber, "Unexpected token count. Expected 2. Found " + tokens.length);
		lastTargetMethodVisitor.visitReturnGroupDefinition(tokens[1]);
	}

	private void visitSimpleConstantDefinition(Visitor visitor, String[] tokens, int lineNumber)
	{ 
		if (tokens.length != 4 && tokens.length != 6)
			throw new UnpickSyntaxException(lineNumber, "Unexpected token count. Expected 4 or 6. Found " + tokens.length);
		if (tokens.length > 4)
		{
			visitor.visitSimpleConstantDefinition(tokens[1], tokens[2], tokens[3], tokens[4], tokens[5]);
		}
		else
		{
		    visitor.visitSimpleConstantDefinition(tokens[1], tokens[2], tokens[3], null, null);
		}
	}
	
	private void visitFlagConstantDefinition(Visitor visitor, String[] tokens, int lineNumber)
	{ 
		if (tokens.length != 4 && tokens.length != 6)
			throw new UnpickSyntaxException(lineNumber, "Unexpected token count. Expected 4 or 6. Found " + tokens.length);
		if (tokens.length > 4)
		{
			visitor.visitFlagConstantDefinition(tokens[1], tokens[2], tokens[3], tokens[4], tokens[5]);
		}
		else
		{
		    visitor.visitFlagConstantDefinition(tokens[1], tokens[2], tokens[3], null, null);
		}
	}
	
	private void visitTargetMethodDefinition(Visitor visitor, String[] tokens, int lineNumber)
	{ 
		if (tokens.length != 4)
			throw new UnpickSyntaxException(lineNumber, "Unexpected token count. Expected 4. Found " + tokens.length);
		if (lastTargetMethodVisitor != null)
			lastTargetMethodVisitor.endVisit();
		lastTargetMethodVisitor = visitor.visitTargetMethodDefinition(tokens[1], tokens[2], tokens[3]);
		if (lastTargetMethodVisitor == null) //Handle nulls as specified in the JavaDoc
			lastTargetMethodVisitor = DEFAULT;
	}

	private String stripComment(String in)
	{
		int c = in.indexOf('#');
		return c == -1 ? in : in.substring(0, c);
	}

	@Override
	public void close() throws IOException
	{
		definitionsStream.close();		
	}
	
	private static final TargetMethodDefinitionVisitor DEFAULT = new TargetMethodDefinitionVisitor() {};
	/**
	 * A visitor for visiting the structure of <a href="https://github.com/Daomephsta/unpick/wiki/Unpick-Format">.unpick v2 format</a> text 
	 * @author Daomephsta
	 */
	public static interface Visitor
	{
		/**Visits the start of the file. This is the first method called.**/
		public default void startVisit() {}
		
		/**
		 * Visits the line number.
		 * @param lineNumber the number of the line that is about to be visited.
		 */
		public default void visitLineNumber(int lineNumber) {};
		
		/**
		 * Visits a simple constant definition (start token {@code constant}).<br>
		 * {@code value} and {@code descriptor} will either both have a value or both be null.
		 * @param group the id of the constant's constant group.
		 * @param owner the internal name of the constant's owner class.
		 * @param name the constant's Java identifier.
		 * @param value the constant's value as a {@code String}, or null if it is not specified (will be resolved at runtime).
		 * @param descriptor the constant's descriptor, or null if it is not specified (will be resolved at runtime).
		 */
		public default void visitSimpleConstantDefinition(String group, String owner, String name, String value, String descriptor) {}

		/**
		 * Visits a flag constant definition (start token {@code flag}).<br>
		 * {@code value} and {@code descriptor} will either both have a value or both be null.
		 * @param group the id of the constant's constant group.
		 * @param owner the internal name of the constant's owner class.
		 * @param name the constant's Java identifier.
		 * @param value the constant's value as a {@code String}, or null if it is not specified (will be resolved at runtime).
		 * @param descriptor the constant's descriptor, or null if it is not specified (will be resolved at runtime).
		 */
		public default void visitFlagConstantDefinition(String group, String owner, String name, String value, String descriptor) {}
		
		/**
		 * Visits a target method definition (start token {@code target_method}).
		 * @param owner the internal name of the method's owner class.
		 * @param name the method's Java identifier.
		 * @param descriptor the method's descriptor.
		 * @return an instance of {@code TargetMethodDefinitionVisitor} that should visit the parameter and return groups of the 
		 * target method definition, or null if they should not be visited.
		 */
		public default TargetMethodDefinitionVisitor visitTargetMethodDefinition(String owner, String name, String descriptor) 
		{
			return DEFAULT;
		}
		
		/**Visits the end of the file. This is the last method called.**/
		public default void endVisit() {}
	}
	
	public static interface TargetMethodDefinitionVisitor
	{	
		/**
		 * Visits a parameter group definition (start token {@code param}).
		 * @param parameterIndex the index of the parameter  
		 * @param group the id of the constant group  
		 */
		public default void visitParameterGroupDefinition(int parameterIndex, String group) {}
		
		/**
		 * Visits a return group definition (start token {@code return}).  
		 * @param group the id of the constant group  
		 */
		public default void visitReturnGroupDefinition(String group) {}
		
		/**Visits the end of the method definition. This is the last method called.**/
		public default void endVisit() {}
	}
}

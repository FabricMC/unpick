package daomephsta.unpick.constantmappers.datadriven.parser;

/**
 * Thrown when a syntax error is found in a .unpick file.
 * @author Daomephsta
 */
public class UnpickSyntaxException extends RuntimeException {
	private static final long serialVersionUID = -86704276968539185L;

	public UnpickSyntaxException(int lineNumber, String message, Throwable cause) {
		super("Line " + lineNumber + ": " + message, cause);
	}

	public UnpickSyntaxException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnpickSyntaxException(int lineNumber, String message) {
		super("Line " + lineNumber + ": " + message);
	}

	public UnpickSyntaxException(String message) {
		super(message);
	}
}

package daomephsta.unpick.constantmappers.datadriven.parser;

import org.jetbrains.annotations.Nullable;

/**
 * Thrown when a syntax error is found in a .unpick file.
 * @author Daomephsta
 */
public class UnpickSyntaxException extends RuntimeException {
	private static final long serialVersionUID = -86704276968539185L;

	@Nullable
	private final Integer lineNumber;
	@Nullable
	private final Integer columnNumber;

	public UnpickSyntaxException(int lineNumber, int columnNumber, String message, Throwable cause) {
		super("Line " + lineNumber + ":" + columnNumber + ": " + message, cause);
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
	}

	public UnpickSyntaxException(int lineNumber, String message, Throwable cause) {
		super("Line " + lineNumber + ": " + message, cause);
		this.lineNumber = lineNumber;
		this.columnNumber = null;
	}

	public UnpickSyntaxException(String message, Throwable cause) {
		super(message, cause);
		this.lineNumber = null;
		this.columnNumber = null;
	}

	public UnpickSyntaxException(int lineNumber, int columnNumber, String message) {
		super("Line " + lineNumber + ":" + columnNumber + ": " + message);
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
	}

	public UnpickSyntaxException(int lineNumber, String message) {
		super("Line " + lineNumber + ": " + message);
		this.lineNumber = lineNumber;
		this.columnNumber = null;
	}

	public UnpickSyntaxException(String message) {
		super(message);
		this.lineNumber = null;
		this.columnNumber = null;
	}

	@Nullable
	public Integer getLineNumber() {
		return lineNumber;
	}

	@Nullable
	public Integer getColumnNumber() {
		return columnNumber;
	}
}

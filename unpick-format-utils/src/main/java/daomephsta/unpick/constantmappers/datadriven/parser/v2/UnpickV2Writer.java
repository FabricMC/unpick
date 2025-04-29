package daomephsta.unpick.constantmappers.datadriven.parser.v2;

import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.TargetMethodDefinitionVisitor;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.Visitor;

/**
 * A visitor that generates .unpick v2 format text. Useful for programmatically writing .unpick v2 format files;
 * or remapping them, when used as the delegate for an instance of {@link UnpickV2Remapper}.
 * @author Daomephsta
 */
public class UnpickV2Writer implements Visitor {
	private final StringBuilder writeBuffer = new StringBuilder();

	@Override
	public void startVisit() {
		if (writeBuffer.length() != 0) {
			return;
		}

		writeBuffer.append("v2").append(System.lineSeparator());
	}

	@Override
	public void visitFlagConstantDefinition(String group, String owner, String name, String value, String descriptor) {
		appendJoining(writeBuffer, " ", "flag", group, owner, name);
		if (value != null && descriptor != null) {
			appendJoining(writeBuffer.append(' '), " ", value, descriptor);
		} else if (!(value == null && descriptor == null)) {
			throw new IllegalArgumentException("value and descriptor must both have a value or both be null");
		}
		writeBuffer.append(System.lineSeparator());
	}

	@Override
	public void visitSimpleConstantDefinition(String group, String owner, String name, String value, String descriptor) {
		appendJoining(writeBuffer, " ", "constant", group, owner, name);
		if (value != null && descriptor != null) {
			appendJoining(writeBuffer.append(' '), " ", value, descriptor);
		} else if (!(value == null && descriptor == null)) {
			throw new IllegalArgumentException("value and descriptor must both have a value or both be null");
		}
		writeBuffer.append(System.lineSeparator());
	}

	@Override
	public TargetMethodDefinitionVisitor visitTargetMethodDefinition(String owner, String name, String descriptor) {
		appendJoining(writeBuffer, " ", "target_method", owner, name, descriptor).append(System.lineSeparator());
		return new TargetMethodWriter(writeBuffer);
	}

	public String getOutput() {
		return writeBuffer.toString();
	}

	private static class TargetMethodWriter implements TargetMethodDefinitionVisitor {
		private final StringBuilder writeBuffer;

		TargetMethodWriter(StringBuilder writeBuffer) {
			this.writeBuffer = writeBuffer;
		}

		@Override
		public void visitParameterGroupDefinition(int parameterIndex, String group) {
			writeBuffer.append("\t");
			appendJoining(writeBuffer, " ", "param", Integer.toString(parameterIndex), group);
			writeBuffer.append(System.lineSeparator());
		}

		@Override
		public void visitReturnGroupDefinition(String group) {
			writeBuffer.append("\t");
			appendJoining(writeBuffer, " ", "return", group);
			writeBuffer.append(System.lineSeparator());
		}
	}

	private static StringBuilder appendJoining(StringBuilder thisArg, CharSequence delimiter, String... elements) {
		thisArg.append(elements[0]);
		for (int e = 1; e < elements.length; e++) {
			thisArg.append(delimiter).append(elements[e]);
		}
		return thisArg;
	}
}

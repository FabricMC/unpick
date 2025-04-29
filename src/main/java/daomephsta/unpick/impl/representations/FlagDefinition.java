package daomephsta.unpick.impl.representations;

import org.objectweb.asm.Type;

import daomephsta.unpick.constantmappers.datadriven.parser.UnpickSyntaxException;
import daomephsta.unpick.impl.IntegerType;

/**
 * Represents a flag field. The value and descriptor may be
 * lazily resolved at runtime.
 * @author Daomephsta
 */
public class FlagDefinition extends AbstractConstantDefinition<FlagDefinition> {
	/**
	 * Constructs an instance of FlagDefinition that will
	 * have its value and descriptor lazily resolved.
	 * @param owner the internal name of the class that owns
	 * the represented flag.
	 * @param name the name of the represented flag.
	 */
	public FlagDefinition(String owner, String name) {
		super(owner, name);
	}

	/**
	 * Constructs an instance of FlagDefinition with the
	 * specified value and descriptor.
	 * @param owner the internal name of the class that owns
	 * the represented flag.
	 * @param name the name of the represented flag.
	 * @param descriptor the descriptor of the represented flag.
	 * @param valueString the value of the the represented flag, as a String.
	 */
	public FlagDefinition(String owner, String name, Type descriptor, String valueString) {
		super(owner, name, descriptor, valueString);
	}

	@Override
	protected Number parseValue(String valueString) {
		try {
			return IntegerType.from(descriptor).parse(valueString);
		} catch (IllegalArgumentException e) {
			throw new UnpickSyntaxException("Cannot parse value " + valueString + " with descriptor " + descriptor, e);
		}
	}

	@Override
	protected void setValue(Object value) throws ResolutionException {
		try {
			// Will throw if value is not of an integral type
			IntegerType.from(value);
			this.value = value;
		} catch (IllegalArgumentException e) {
			throw new ResolutionException(value + " is not of a valid flag type", e);
		}
	}

	@Override
	public Number getValue() {
		return (Number) super.getValue();
	}

	@Override
	public String toString() {
		if (isResolved()) {
			return String.format("FlagDefinition {Qualified Name: %s.%s, Descriptor: %s, Bits: %s}",
					owner, name, descriptor, Long.toBinaryString(getValue().longValue()));
		} else {
			return String.format("FlagDefinition {Qualified Name: %s.%s}", owner, name);
		}
	}
}

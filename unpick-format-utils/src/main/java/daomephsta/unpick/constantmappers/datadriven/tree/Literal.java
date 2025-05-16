package daomephsta.unpick.constantmappers.datadriven.tree;

public sealed interface Literal {
	record Integer(int value, int radix) implements Literal {
		public Integer(int value) {
			this(value, 10);
		}
	}

	record Long(long value, int radix) implements Literal {
		public Long(long value) {
			this(value, 10);
		}
	}

	record Float(float value) implements Literal {
	}

	record Double(double value) implements Literal {
	}

	record Character(char value) implements Literal {
	}

	record String(java.lang.String value) implements Literal {
	}
}

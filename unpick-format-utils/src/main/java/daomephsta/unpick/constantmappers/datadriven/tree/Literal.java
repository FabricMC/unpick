package daomephsta.unpick.constantmappers.datadriven.tree;

public abstract class Literal
{
	private Literal()
	{
	}

	public static abstract class ConstantKey extends Literal
	{
		private ConstantKey()
		{
		}
	}

	public static final class Integer extends Literal
	{
		public final int value;
		public final int radix;

		public Integer(int value)
		{
			this(value, 10);
		}

		public Integer(int value, int radix)
		{
			this.value = value;
			this.radix = radix;
		}
	}

	public static final class Long extends ConstantKey
	{
		public final long value;
		public final int radix;

		public Long(long value)
		{
			this(value, 10);
		}

		public Long(long value, int radix)
		{
			this.value = value;
			this.radix = radix;
		}
	}

	public static final class Float extends Literal
	{
		public final float value;

		public Float(float value)
		{
			this.value = value;
		}
	}

	public static final class Double extends ConstantKey
	{
		public final double value;

		public Double(double value)
		{
			this.value = value;
		}
	}

	public static final class Character extends Literal
	{
		public final char value;

		public Character(char value)
		{
			this.value = value;
		}
	}

	public static final class String extends ConstantKey
	{
		public final java.lang.String value;

		public String(java.lang.String value)
		{
			this.value = value;
		}
	}
}

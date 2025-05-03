package pkg;

public class TestNonStaticConstants {
	public final int foo = Constants.blackBoxInt(3);
	public final String bar = Constants.blackBoxString("Hello, World!");
	public final Class<?> baz = Constants.blackBoxClass(String.class);
	public final String nullConstant = Constants.blackBoxString(null);

	public void test() {
		Constants.consumeInt(foo);
		Constants.consumeString(bar);
		Constants.consumeClass(baz);
		Constants.consumeString(nullConstant);
	}

	public void testOther(TestNonStaticConstants other) {
		Constants.consumeInt(other.foo);
		Constants.consumeString(new TestNonStaticConstants().bar);
	}

	public class TestInner {
		public void test() {
			Constants.consumeInt(foo);
		}

		public class TestInnerInner {
			public void test() {
				Constants.consumeInt(foo);
			}
		}
	}
}

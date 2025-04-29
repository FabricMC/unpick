package daomephsta.unpick.constantmappers.datadriven.parser;

public class Utils {
	public static StringBuilder appendJoining(StringBuilder thisArg, CharSequence delimiter, String... elements) {
		thisArg.append(elements[0]);
		for (int e = 1; e < elements.length; e++) {
			thisArg.append(delimiter).append(elements[e]);
		}
		return thisArg;
	}
}

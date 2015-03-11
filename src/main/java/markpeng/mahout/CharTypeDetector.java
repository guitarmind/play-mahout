package markpeng.mahout;

public class CharTypeDetector {
	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz";

	/**
	 * check if a given char is a chinese character
	 *
	 * @param c
	 * @return
	 */
	public static final boolean isTraditionalChineseCharacter(char c) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
		if (!Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block)
				&& !Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
						.equals(block)
				&& !Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
						.equals(block)) {
			return false;
		}
		try {
			String s = "" + c;
			return s.equals(new String(s.getBytes("MS950"), "MS950"));
		} catch (java.io.UnsupportedEncodingException e) {
			return false;
		}
	}

	public static final boolean isWhileSpace(char c) {
		return Character.isWhitespace(c);
	}

	public static final boolean isLetter(char c) {
		return Character.isLetter(c);
	}

	public static final boolean isDigit(char c) {
		return Character.isDigit(c);
	}

	public static boolean isAlphabetic(char c) {
		return ALPHABET.indexOf(c) != -1;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("[:"
				+ CharTypeDetector.isTraditionalChineseCharacter('['));
		System.out.println("我:"
				+ CharTypeDetector.isTraditionalChineseCharacter('我'));
		System.out.println("【:"
				+ CharTypeDetector.isTraditionalChineseCharacter('【'));
		System.out.println("（:"
				+ CharTypeDetector.isTraditionalChineseCharacter('（'));
		System.out.println("a:" + CharTypeDetector.isAlphabetic('a'));
		System.out.println("』:" + CharTypeDetector.isAlphabetic('』'));

	}
}

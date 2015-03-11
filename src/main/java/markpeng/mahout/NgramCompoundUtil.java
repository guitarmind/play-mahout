package markpeng.mahout;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class NgramCompoundUtil {
	public String gatherGrams(String ngram) {
		String result = null;

		StringTokenizer tokenizer = new StringTokenizer(ngram);
		List<String> tokens = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()) {
			String tmp = tokenizer.nextToken().trim();
			if (tmp.length() > 0)
				tokens.add(tmp);
		}

		for (int i = 0; i < tokens.size() - 1; i++) {
			if (i == 0) {
				if (tokens.size() > 1) {
					char token1LastChar;
					String token1 = tokens.get(i);
					token1LastChar = token1.charAt(token1.length() - 1);
					String token2 = tokens.get(i + 1);
					char token2FirstChar = token2.charAt(0);

					if (token1LastChar == token2FirstChar
							&& !CharTypeDetector.isDigit(token1LastChar)
							&& !CharTypeDetector.isAlphabetic(token1LastChar)) {
						int token1End = token1.length() - 1;
						String compoundGram = token1.substring(0, token1End)
								+ token2;

						if (compoundGram.length() > 0)
							result = compoundGram;
					} else
						result = token1 + " " + token2;
				} else
					result = tokens.get(i);
			} else {
				if (i + 1 < tokens.size()) {
					char token1LastChar;
					// use compounded string as token 1
					String token1 = result;
					token1LastChar = token1.charAt(token1.length() - 1);
					String token2 = tokens.get(i + 1);
					char token2FirstChar = token2.charAt(0);

					if (token1LastChar == token2FirstChar
							&& !CharTypeDetector.isDigit(token1LastChar)
							&& !CharTypeDetector.isAlphabetic(token1LastChar)) {
						int token1End = token1.length() - 1;
						String compoundGram = token1.substring(0, token1End)
								+ token2;

						if (compoundGram.length() > 0)
							result = compoundGram;
					} else
						result = token1 + " " + token2;
				}
			}

		} // end of for loop

		if (result == null)
			result = ngram;

		return result.trim();
	}

	public static void main(String[] args) {
		NgramCompoundUtil util = new NgramCompoundUtil();
		// String ngrams = "拍 立 得";
		String ngrams = "5 倍光 光学 学变 变焦";
		// String ngrams = "sony 索尼";
		// String ngrams = "官方 方标 标配";
		// String ngrams = "数码 码相 相机";
		String result = util.gatherGrams(ngrams);
		System.out.println(result);
	}
}

package markpeng.kaggle.ssr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class TermExtractor {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public void extractCompoundFromQuery(String trainFile, String testFile,
			String outputTrain, String outputTest) throws Exception {

		List<String> detectedCompounds = new ArrayList<String>();

		// System.setOut(new PrintStream(
		// new BufferedOutputStream(
		// new FileOutputStream(
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/query_train_relevance4_title_20150517.txt")),
		// true));

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testFile), "UTF-8"));

		CsvParserSettings settings = new CsvParserSettings();
		settings.setParseUnescapedQuotes(false);
		settings.getFormat().setLineSeparator("\n");
		settings.getFormat().setDelimiter(',');
		settings.getFormat().setQuote('"');
		settings.setHeaderExtractionEnabled(true);
		settings.setEmptyValue("");
		settings.setMaxCharsPerColumn(40960);

		// -------------------------------------------------------------------------------------------
		// Train Data

		try {

			// creates a CSV parser
			CsvParser trainParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			trainParser.beginParsing(trainIn);

			int count = 0;
			String[] tokens;
			while ((tokens = trainParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();
				int medianRelevance = Integer.parseInt(tokens[4]);
				double relevance_variance = Double.parseDouble(tokens[5]);
				// preprocessing
				String cleanQuery = processTextByLucene(getTextFromRawData(query));
				String cleanProductTitle = processTextByLucene(getTextFromRawData(productTitle));
				String cleanProductDesc = processTextByLucene(getTextFromRawData(productDesc));

				List<String> qTokens = getTermsAsListByLucene(cleanQuery);
				List<String> titleTokens = getTermsAsListByLucene(cleanProductTitle);
				List<String> descTokens = getTermsAsListByLucene(cleanProductDesc);

				// fully matched
				// if (medianRelevance >= 2) {

				// create compounds
				List<String> compoundFromT = new ArrayList<String>();
				for (int i = 0; i < titleTokens.size(); i++) {
					if (i + 1 <= titleTokens.size() - 1) {
						if (titleTokens.get(i).length() >= 3
								&& titleTokens.get(i + 1).length() >= 3) {
							String compound = titleTokens.get(i) + " "
									+ titleTokens.get(i + 1);
							if (!compoundFromT.contains(compound))
								compoundFromT.add(compound);
						}
					}
				}
				for (String token : compoundFromT) {
					String tmp = token.replace(" ", "");
					for (String qt : qTokens) {
						if (qt.length() > 3 && qt.length() >= tmp.length()
								&& qt.contains(tmp)) {
							// System.out.println(qt + "=>" + token
							// + " (from Title)");
							if (!detectedCompounds.contains(token))
								detectedCompounds.add(token);
						}
					}
				}

				List<String> compoundFromD = new ArrayList<String>();
				for (int i = 0; i < descTokens.size(); i++) {
					if (i + 1 <= descTokens.size() - 1) {
						if (descTokens.get(i).length() >= 3
								&& descTokens.get(i + 1).length() >= 3) {
							String compound = descTokens.get(i) + " "
									+ descTokens.get(i + 1);
							if (!compoundFromD.contains(compound))
								compoundFromD.add(compound);
						}
					}
				}
				for (String token : compoundFromD) {
					String tmp = token.replace(" ", "");
					for (String qt : qTokens) {
						if (qt.length() > 3 && qt.length() >= tmp.length()
								&& qt.contains(tmp)) {
							// System.out.println(qt + "=>" + token
							// + " (from Desc)");
							if (!detectedCompounds.contains(token))
								detectedCompounds.add(token);
						}
					}
				}

				count++;
			}

			System.out.println("Total train records: " + count);

		} finally {
			trainIn.close();
		}

		// -------------------------------------------------------------------------------------------
		// Test Data

		try {

			// creates a CSV parser
			CsvParser testParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			testParser.beginParsing(testIn);

			int count = 0;
			String[] tokens;
			while ((tokens = testParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();

				// preprocessing
				String cleanQuery = processTextByLucene(getTextFromRawData(query));
				String cleanProductTitle = processTextByLucene(getTextFromRawData(productTitle));
				String cleanProductDesc = processTextByLucene(getTextFromRawData(productDesc));

				List<String> qTokens = getTermsAsListByLucene(cleanQuery);
				List<String> titleTokens = getTermsAsListByLucene(cleanProductTitle);
				List<String> descTokens = getTermsAsListByLucene(cleanProductDesc);

				// create compounds
				List<String> compoundFromT = new ArrayList<String>();
				for (int i = 0; i < titleTokens.size(); i++) {
					if (i + 1 <= titleTokens.size() - 1) {
						if (titleTokens.get(i).length() >= 3
								&& titleTokens.get(i + 1).length() >= 3) {
							String compound = titleTokens.get(i) + " "
									+ titleTokens.get(i + 1);
							if (!compoundFromT.contains(compound))
								compoundFromT.add(compound);
						}
					}
				}
				for (String token : compoundFromT) {
					String tmp = token.replace(" ", "");
					for (String qt : qTokens) {
						if (qt.length() > 3 && qt.length() >= tmp.length()
								&& qt.contains(tmp)) {
							// System.out.println(qt + "=>" + token
							// + " (from Title)");
							if (!detectedCompounds.contains(token))
								detectedCompounds.add(token);
						}
					}
				}

				List<String> compoundFromD = new ArrayList<String>();
				for (int i = 0; i < descTokens.size(); i++) {
					if (i + 1 <= descTokens.size() - 1) {
						if (descTokens.get(i).length() >= 3
								&& descTokens.get(i + 1).length() >= 3) {
							String compound = descTokens.get(i) + " "
									+ descTokens.get(i + 1);
							if (!compoundFromD.contains(compound))
								compoundFromD.add(compound);
						}
					}
				}
				for (String token : compoundFromD) {
					String tmp = token.replace(" ", "");
					for (String qt : qTokens) {
						if (qt.length() > 3 && qt.length() >= tmp.length()
								&& qt.contains(tmp)) {
							// System.out.println(qt + "=>" + token
							// + " (from Desc)");
							if (!detectedCompounds.contains(token))
								detectedCompounds.add(token);
						}
					}
				}

				count++;
			}

			System.out.println("Total test records: " + count);

		} finally {
			testIn.close();
		}

		System.out.println("\n\n[From Title ad Desc]");
		for (String t : detectedCompounds)
			System.out.println(t);

		System.out.flush();
	}

	public void extractCompoundFromTitleAndDescription(String trainFile,
			String testFile, String outputTrain, String outputTest)
			throws Exception {

		List<String> detectedCompounds = new ArrayList<String>();

		// System.setOut(new PrintStream(
		// new BufferedOutputStream(
		// new FileOutputStream(
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/query_train_relevance4_title_20150517.txt")),
		// true));

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testFile), "UTF-8"));

		CsvParserSettings settings = new CsvParserSettings();
		settings.setParseUnescapedQuotes(false);
		settings.getFormat().setLineSeparator("\n");
		settings.getFormat().setDelimiter(',');
		settings.getFormat().setQuote('"');
		settings.setHeaderExtractionEnabled(true);
		settings.setEmptyValue("");
		settings.setMaxCharsPerColumn(40960);

		// -------------------------------------------------------------------------------------------
		// Train Data

		try {

			// creates a CSV parser
			CsvParser trainParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			trainParser.beginParsing(trainIn);

			int count = 0;
			String[] tokens;
			while ((tokens = trainParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();
				int medianRelevance = Integer.parseInt(tokens[4]);
				double relevance_variance = Double.parseDouble(tokens[5]);
				// preprocessing
				String cleanQuery = processTextByLucene(getTextFromRawData(query));
				String cleanProductTitle = processTextByLucene(getTextFromRawData(productTitle));
				String cleanProductDesc = processTextByLucene(getTextFromRawData(productDesc));

				List<String> qTokens = getTermsAsListByLucene(cleanQuery);
				List<String> titleTokens = getTermsAsListByLucene(cleanProductTitle);
				List<String> descTokens = getTermsAsListByLucene(cleanProductDesc);

				// fully matched
				// if (medianRelevance >= 2) {

				// create compounds
				List<String> compoundFromT = new ArrayList<String>();
				for (int i = 0; i < qTokens.size(); i++) {
					if (i + 1 <= qTokens.size() - 1) {
						if (qTokens.get(i).length() >= 3
								&& qTokens.get(i + 1).length() >= 3) {
							String compound = qTokens.get(i) + " "
									+ qTokens.get(i + 1);
							if (!compoundFromT.contains(compound))
								compoundFromT.add(compound);
						}
					}
				}
				for (String token : compoundFromT) {
					String tmp = token.replace(" ", "");
					for (String qt : titleTokens) {
						if (qt.length() > 3 && qt.length() >= tmp.length()
								&& qt.contains(tmp)) {
							// System.out.println(qt + "=>" + token
							// + " (from Title)");
							if (!detectedCompounds.contains(token))
								detectedCompounds.add(token);
						}
					}
				}

				List<String> compoundFromD = new ArrayList<String>();
				for (int i = 0; i < qTokens.size(); i++) {
					if (i + 1 <= qTokens.size() - 1) {
						if (qTokens.get(i).length() >= 3
								&& qTokens.get(i + 1).length() >= 3) {
							String compound = qTokens.get(i) + " "
									+ qTokens.get(i + 1);
							if (!compoundFromD.contains(compound))
								compoundFromD.add(compound);
						}
					}
				}
				for (String token : compoundFromD) {
					String tmp = token.replace(" ", "");
					for (String qt : descTokens) {
						if (qt.length() > 3 && qt.length() >= tmp.length()
								&& qt.contains(tmp)) {
							// System.out.println(qt + "=>" + token
							// + " (from Desc)");
							if (!detectedCompounds.contains(token))
								detectedCompounds.add(token);
						}
					}
				}

				count++;
			}

			System.out.println("Total train records: " + count);

		} finally {
			trainIn.close();
		}

		// -------------------------------------------------------------------------------------------
		// Test Data

		try {

			// creates a CSV parser
			CsvParser testParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			testParser.beginParsing(testIn);

			int count = 0;
			String[] tokens;
			while ((tokens = testParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();

				// preprocessing
				String cleanQuery = processTextByLucene(getTextFromRawData(query));
				String cleanProductTitle = processTextByLucene(getTextFromRawData(productTitle));
				String cleanProductDesc = processTextByLucene(getTextFromRawData(productDesc));

				List<String> qTokens = getTermsAsListByLucene(cleanQuery);
				List<String> titleTokens = getTermsAsListByLucene(cleanProductTitle);
				List<String> descTokens = getTermsAsListByLucene(cleanProductDesc);

				// create compounds
				List<String> compoundFromT = new ArrayList<String>();
				for (int i = 0; i < qTokens.size(); i++) {
					if (i + 1 <= qTokens.size() - 1) {
						if (qTokens.get(i).length() >= 3
								&& qTokens.get(i + 1).length() >= 3) {
							String compound = qTokens.get(i) + " "
									+ qTokens.get(i + 1);
							if (!compoundFromT.contains(compound))
								compoundFromT.add(compound);
						}
					}
				}
				for (String token : compoundFromT) {
					String tmp = token.replace(" ", "");
					for (String qt : titleTokens) {
						if (qt.length() > 3 && qt.length() >= tmp.length()
								&& qt.contains(tmp)) {
							// System.out.println(qt + "=>" + token
							// + " (from Title)");
							if (!detectedCompounds.contains(token))
								detectedCompounds.add(token);
						}
					}
				}

				List<String> compoundFromD = new ArrayList<String>();
				for (int i = 0; i < qTokens.size(); i++) {
					if (i + 1 <= qTokens.size() - 1) {
						if (qTokens.get(i).length() >= 3
								&& qTokens.get(i + 1).length() >= 3) {
							String compound = qTokens.get(i) + " "
									+ qTokens.get(i + 1);
							if (!compoundFromD.contains(compound))
								compoundFromD.add(compound);
						}
					}
				}
				for (String token : compoundFromD) {
					String tmp = token.replace(" ", "");
					for (String qt : descTokens) {
						if (qt.length() > 3 && qt.length() >= tmp.length()
								&& qt.contains(tmp)) {
							// System.out.println(qt + "=>" + token
							// + " (from Desc)");
							if (!detectedCompounds.contains(token))
								detectedCompounds.add(token);
						}
					}
				}
				count++;
			}

			System.out.println("Total test records: " + count);

		} finally {
			testIn.close();
		}

		System.out.println("\n\n[From Title ad Desc]");
		for (String t : detectedCompounds)
			System.out.println(t);

		System.out.flush();
	}

	public void extractAllTerms(String trainFile, String testFile,
			String outputTrain, String outputTest) throws Exception {

		StringBuffer resultStr = new StringBuffer();

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testFile), "UTF-8"));

		BufferedWriter trainOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTrain, false), "UTF-8"));

		BufferedWriter testOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTest, false), "UTF-8"));

		CsvParserSettings settings = new CsvParserSettings();
		settings.setParseUnescapedQuotes(false);
		settings.getFormat().setLineSeparator("\n");
		settings.getFormat().setDelimiter(',');
		settings.getFormat().setQuote('"');
		settings.setHeaderExtractionEnabled(true);
		settings.setEmptyValue("");
		settings.setMaxCharsPerColumn(40960);

		// -------------------------------------------------------------------------------------------
		// Train Data

		// create headers
		resultStr
				.append("\"id\",\"query\",\"product_title\",\"product_description\",\""
						+ "median_relevance\",\"relevance_variance\"");
		resultStr.append(newLine);

		try {
			// creates a CSV parser
			CsvParser trainParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			trainParser.beginParsing(trainIn);

			int count = 0;
			String[] tokens;
			while ((tokens = trainParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();
				int medianRelevance = Integer.parseInt(tokens[4]);
				double relevance_variance = Double.parseDouble(tokens[5]);
				// preprocessing
				String cleanQuery = processTextByLucene(getTextFromRawData(query));
				String cleanProductTitle = processTextByLucene(getTextFromRawData(productTitle));
				String cleanProductDesc = processTextByLucene(getTextFromRawData(productDesc));

				resultStr.append("\"" + id + "\",\"" + cleanQuery + "\",\""
						+ cleanProductTitle + "\",\"" + cleanProductDesc
						+ "\",\"" + medianRelevance + "\",\""
						+ relevance_variance + "\"");
				resultStr.append(newLine);

				if (resultStr.length() >= BUFFER_LENGTH) {
					trainOut.write(resultStr.toString());
					trainOut.flush();
					resultStr.setLength(0);
				}

				count++;
			}

			System.out.println("Total train records: " + count);

		} finally {
			trainIn.close();

			trainOut.write(resultStr.toString());
			trainOut.flush();
			trainOut.close();
			resultStr.setLength(0);
		}

		// -------------------------------------------------------------------------------------------
		// Test Data

		resultStr.setLength(0);
		// create headers
		resultStr
				.append("\"id\",\"query\",\"product_title\",\"product_description\"");
		resultStr.append(newLine);

		try {
			// creates a CSV parser
			CsvParser testParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			testParser.beginParsing(testIn);

			int count = 0;
			String[] tokens;
			while ((tokens = testParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();
				// preprocessing
				String cleanQuery = processTextByLucene(getTextFromRawData(query));
				String cleanProductTitle = processTextByLucene(getTextFromRawData(productTitle));
				String cleanProductDesc = processTextByLucene(getTextFromRawData(productDesc));

				resultStr
						.append("\"" + id + "\",\"" + cleanQuery + "\",\""
								+ cleanProductTitle + "\",\""
								+ cleanProductDesc + "\"");
				resultStr.append(newLine);

				if (resultStr.length() >= BUFFER_LENGTH) {
					testOut.write(resultStr.toString());
					testOut.flush();
					resultStr.setLength(0);
				}

				count++;
			}

			System.out.println("Total test records: " + count);

		} finally {
			testIn.close();

			testOut.write(resultStr.toString());
			testOut.flush();
			testOut.close();
			resultStr.setLength(0);
		}

		System.out.flush();
	}

	public String getTextFromRawData(String raw) {
		String result = raw;

		Document doc = Jsoup.parse(raw);
		Element body = doc.body();
		String plainText = body.text().trim();
		if (plainText.length() > 2) {
			// System.out.println(plainText);
			result = plainText;
		}

		return result;
	}

	private Hashtable<String, Integer> getTermFreqByLucene(String text)
			throws IOException {
		Hashtable<String, Integer> result = new Hashtable<String, Integer>();

		Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
				.getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
		// ts = new PorterStemFilter(ts);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					if (isAllEnglish(word) && word.length() > 3) {
						if (result.get(word) == null)
							result.put(word, 1);
						else {
							result.put(word, result.get(word) + 1);
						}
					}

					wordCount++;
				}
			}

		} finally {
			// Fixed error : close ts:TokenStream
			ts.end();
			ts.close();
		}

		return result;
	}

	private List<String> getTermsAsListByLucene(String text) throws IOException {
		List<String> result = new ArrayList<String>();

		Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
				.getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
		// ts = new PorterStemFilter(ts);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					if (isAllEnglish(word)) {
						if (!result.contains(word))
							result.add(word);
					}

					wordCount++;
				}
			}

		} finally {
			// Fixed error : close ts:TokenStream
			ts.end();
			ts.close();
		}

		return result;
	}

	public String processTextByLucene(String text) throws IOException {
		String result = text;

		StringBuffer postText = new StringBuffer();
		Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
				.getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
		int flags = WordDelimiterFilter.SPLIT_ON_NUMERICS
				| WordDelimiterFilter.SPLIT_ON_CASE_CHANGE
				| WordDelimiterFilter.GENERATE_NUMBER_PARTS
				| WordDelimiterFilter.GENERATE_WORD_PARTS;
		ts = new WordDelimiterFilter(ts, flags, null);
		ts = new LowerCaseFilter(Version.LUCENE_46, ts);
		// ts = new PorterStemFilter(ts);
		// ts = new DictionaryCompoundWordTokenFilter(Version.LUCENE_46, ts,
		// new CharArraySet(Version.LUCENE_46, dictionary, true), 6, 4,
		// 10, false);

		// less aggressive, better than PorterStemFilter!
		ts = new KStemFilter(ts);

		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					postText.append(word + " ");
					// System.out.println(word);

					wordCount++;
				}
			}

			String finalText = postText.toString().trim().replace("'", "");
			if (finalText.length() > 1)
				result = finalText;

		} finally {
			// Fixed error : close ts:TokenStream
			ts.end();
			ts.close();
		}

		return result;
	}

	public boolean isAllEnglish(String text) {
		boolean result = true;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!Character.isAlphabetic(c) && c != '-') {
				result = false;
				break;
			}
		}

		return result;
	}

	public static void main(String[] args) throws Exception {
		args = new String[5];
		args[0] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train.csv";
		args[1] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test.csv";
		args[2] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_markpeng.csv";
		args[3] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test_filterred_markpeng.csv";
		args[4] = "/home/markpeng/Share/Kaggle/Search Results Relevance/JOrtho/dictionary_en_2015_05/IncludedWords.txt";

		if (args.length < 5) {
			System.out
					.println("Arguments: [train.csv] [test.csv] [output train] [output test] [dic path]");
			return;
		}
		String trainFile = args[0];
		String testFile = args[1];
		// String[] featureFiles = args[2].split("\\|");
		String outputTrain = args[2];
		String outputTest = args[3];
		String dicPath = args[4];

		TermExtractor worker = new TermExtractor();
		// worker.extractAllTerms(trainFile, testFile, outputTrain, outputTest);
		// worker.extractCompoundFromQuery(trainFile, testFile, outputTrain,
		// outputTest);
		worker.extractCompoundFromTitleAndDescription(trainFile, testFile,
				outputTrain, outputTest);
	}
}

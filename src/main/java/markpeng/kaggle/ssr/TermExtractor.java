package markpeng.kaggle.ssr;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
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

	public void extractSmallerWordsBaseOnQuery(String trainFile,
			String testFile, String outputTrain, String outputTest)
			throws Exception {

		TreeSet<String> detectedCompounds = new TreeSet<String>();

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

				// create compounds
				for (String qt : qTokens) {
					for (String token : titleTokens) {
						if (qt.length() >= 3 && token.length() >= 3
								&& qt.length() <= 10 && token.length() <= 10
								&& qt.length() < token.length()) {
							if (token.startsWith(qt) || token.endsWith(qt)) {
								String compound = token;

								boolean valid = false;
								if (token.startsWith(qt)) {
									String suffix = token.substring(token
											.indexOf(qt) + qt.length());
									if (suffix.length() >= 3) {
										compound = qt + " " + suffix;
										valid = true;
									}
								} else {
									String prefix = token.substring(0,
											token.indexOf(qt));

									if (prefix.length() >= 3) {
										compound = prefix + " " + qt;
										valid = true;
									}
								}

								if (valid
										&& !detectedCompounds
												.contains(compound))
									detectedCompounds.add(compound);
							}
						}
					}
				}

				for (String qt : qTokens) {
					for (String token : descTokens) {
						if (qt.length() >= 3 && token.length() >= 3
								&& qt.length() <= 10 && token.length() <= 10
								&& qt.length() < token.length()) {
							if (token.startsWith(qt) || token.endsWith(qt)) {
								String compound = token;

								boolean valid = false;
								if (token.startsWith(qt)) {
									String suffix = token.substring(token
											.indexOf(qt) + qt.length());
									if (suffix.length() >= 3) {
										compound = qt + " " + suffix;
										valid = true;
									}
								} else {
									String prefix = token.substring(0,
											token.indexOf(qt));

									if (prefix.length() >= 3) {
										compound = prefix + " " + qt;
										valid = true;
									}
								}

								if (valid
										&& !detectedCompounds
												.contains(compound))
									detectedCompounds.add(compound);
							}
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
				for (String qt : qTokens) {
					for (String token : titleTokens) {
						if (qt.length() >= 3 && token.length() >= 3
								&& qt.length() <= 10 && token.length() <= 10
								&& qt.length() < token.length()) {
							if (token.startsWith(qt) || token.endsWith(qt)) {
								String compound = token;

								boolean valid = false;
								if (token.startsWith(qt)) {
									String suffix = token.substring(token
											.indexOf(qt) + qt.length());
									if (suffix.length() >= 3) {
										compound = qt + " " + suffix;
										valid = true;
									}
								} else {
									String prefix = token.substring(0,
											token.indexOf(qt));

									if (prefix.length() >= 3) {
										compound = prefix + " " + qt;
										valid = true;
									}
								}

								if (valid
										&& !detectedCompounds
												.contains(compound))
									detectedCompounds.add(compound);
							}
						}
					}
				}

				for (String qt : qTokens) {
					for (String token : descTokens) {
						if (qt.length() >= 3 && token.length() >= 3
								&& qt.length() <= 10 && token.length() <= 10
								&& qt.length() < token.length()) {
							if (token.startsWith(qt) || token.endsWith(qt)) {
								String compound = token;

								boolean valid = false;
								if (token.startsWith(qt)) {
									String suffix = token.substring(token
											.indexOf(qt) + qt.length());
									if (suffix.length() >= 3) {
										compound = qt + " " + suffix;
										valid = true;
									}
								} else {
									String prefix = token.substring(0,
											token.indexOf(qt));

									if (prefix.length() >= 3) {
										compound = prefix + " " + qt;
										valid = true;
									}
								}

								if (valid
										&& !detectedCompounds
												.contains(compound))
									detectedCompounds.add(compound);
							}
						}
					}
				}

				count++;
			}

			System.out.println("Total test records: " + count);

		} finally {
			testIn.close();
		}

		System.out.println("\n\n[From Title and Desc]");
		for (String t : detectedCompounds)
			System.out.println(t);

		System.out.flush();
	}

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

	public void extractAllTermsInQuery(String trainFile, String testFile,
			String outputFile) throws Exception {

		List<String> queryTerms = new ArrayList<String>();

		TreeSet<String> allQuery = new TreeSet<String>();
		TreeMap<String, Integer> queryInTrain = new TreeMap<String, Integer>();
		TreeMap<String, Integer> queryInTest = new TreeMap<String, Integer>();

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
				for (String q : qTokens) {
					if (!queryTerms.contains(q))
						queryTerms.add(q);
				}

				if (!queryInTrain.containsKey(cleanQuery))
					queryInTrain.put(cleanQuery, 1);
				else
					queryInTrain.put(cleanQuery,
							queryInTrain.get(cleanQuery) + 1);
				if (!allQuery.contains(cleanQuery))
					allQuery.add(cleanQuery);

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
				for (String q : qTokens) {
					if (!queryTerms.contains(q))
						queryTerms.add(q);
				}

				if (!queryInTest.containsKey(cleanQuery))
					queryInTest.put(cleanQuery, 1);
				else
					queryInTest
							.put(cleanQuery, queryInTest.get(cleanQuery) + 1);
				if (!allQuery.contains(cleanQuery))
					allQuery.add(cleanQuery);

				count++;
			}

			System.out.println("Total test records: " + count);

		} finally {
			testIn.close();
		}

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFile, false), "UTF-8"));
		// System.out.println("\n\n[From Query]");
		StringBuffer outputStr = new StringBuffer();
		for (String t : queryTerms) {
			// System.out.println(t);
			outputStr.append(t + newLine);
		}
		out.write(outputStr.toString());
		out.flush();
		out.close();

		// query statistics
		System.out.println("\n\n[From Train]");
		for (String q : allQuery) {
			// if (!queryInTrain.containsKey(q))
			System.out.println(q + ": " + queryInTrain.get(q));
		}
		System.out.println("\n\n[From Test]");
		for (String q : allQuery) {
			// if (!queryInTest.containsKey(q))
			System.out.println(q + ": " + queryInTest.get(q));
		}
		System.out.println("\n\nCounf of all query: " + allQuery.size());

		System.out.flush();
	}

	public void extractDistinctQuery(String trainFile) throws Exception {
		TreeSet<String> allQuery = new TreeSet<String>();

		System.setOut(new PrintStream(
				new BufferedOutputStream(
						new FileOutputStream(
								"/home/markpeng/Share/Kaggle/Search Results Relevance/dintinct_query_origin_20150613.txt")),
				true));

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

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

			String[] tokens;
			while ((tokens = trainParser.parseNext()) != null) {
				String query = tokens[1].replace("\"", "").trim();

				if (!allQuery.contains(query))
					allQuery.add(query);
			}

		} finally {
			trainIn.close();
		}

		for (String q : allQuery) {
			System.out.println(q);
		}

		System.out.flush();
	}

	public void extractBigramWithDigitFromQuery(String trainFile,
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

				List<String> qTokens = getNgramTermsAsListByLucene(cleanQuery,
						2);

				// create compounds
				for (String token : qTokens) {
					if (cleanProductTitle.contains(token)
							|| cleanProductDesc.contains(token))
						if (!detectedCompounds.contains(token))
							detectedCompounds.add(token);
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

				List<String> qTokens = getNgramTermsAsListByLucene(cleanQuery,
						2);

				// create compounds
				for (String token : qTokens) {
					if (cleanProductTitle.contains(token)
							|| cleanProductDesc.contains(token))
						if (!detectedCompounds.contains(token))
							detectedCompounds.add(token);
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

	public void extractBigramTopicCompoundFromTitleMappingInDescription(
			String trainFile, String testFile, String outputTrain,
			String outputTest) throws Exception {
		List<String> detectedCompounds = new ArrayList<String>();
		Hashtable<String, Integer> docFreqInTrain = new Hashtable<String, Integer>();
		Hashtable<String, Integer> docFreqInTest = new Hashtable<String, Integer>();

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
				String cleanProductTitle = processTextByLucene(getTextFromRawData(productTitle));
				String cleanProductDesc = processTextByLucene(getTextFromRawData(productDesc));

				List<String> titleTokens = getNgramTermsAsListByLucene(
						cleanProductTitle, 2, 3, true, false);
				List<String> descTokens = getNgramTermsAsListByLucene(
						cleanProductDesc, 2, 3, true, false);

				// create compounds
				for (String token : titleTokens) {
					if (cleanProductDesc.contains(token)) {
						if (!detectedCompounds.contains(token))
							detectedCompounds.add(token);

						if (!docFreqInTrain.containsKey(token))
							docFreqInTrain.put(token, 1);
						else
							docFreqInTrain.put(token,
									docFreqInTrain.get(token) + 1);
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
				String cleanProductTitle = processTextByLucene(getTextFromRawData(productTitle));
				String cleanProductDesc = processTextByLucene(getTextFromRawData(productDesc));

				List<String> titleTokens = getNgramTermsAsListByLucene(
						cleanProductTitle, 2, 3, true, false);
				List<String> descTokens = getNgramTermsAsListByLucene(
						cleanProductDesc, 2, 3, true, false);

				// create compounds
				for (String token : titleTokens) {
					if (cleanProductDesc.contains(token)) {
						if (!detectedCompounds.contains(token))
							detectedCompounds.add(token);

						if (!docFreqInTest.containsKey(token))
							docFreqInTest.put(token, 1);
						else
							docFreqInTest.put(token,
									docFreqInTest.get(token) + 1);
					}
				}

				count++;
			}

			System.out.println("Total test records: " + count);

		} finally {
			testIn.close();
		}

		System.out.println("\n\n[From Title and Desc]");
		for (String t : detectedCompounds) {
			if (docFreqInTrain.containsKey(t) && docFreqInTest.containsKey(t)) {
				if (docFreqInTrain.get(t) >= 10 && docFreqInTest.get(t) >= 10)
					System.out.println(t + " (trainDF=" + docFreqInTrain.get(t)
							+ ", testDF=" + docFreqInTest.get(t) + ")");
			}
		}

		System.out.flush();
	}

	public void extractKeywordFromTitleAndDescriptionByScore(String trainFile,
			String testFile, String outputFeatureFile) throws Exception {
		TreeSet<String> allUniqueTokens = new TreeSet<String>();

		TreeSet<String> score1UniqueTokens = new TreeSet<String>();
		TreeSet<String> score2UniqueTokens = new TreeSet<String>();
		TreeSet<String> score3UniqueTokens = new TreeSet<String>();
		TreeSet<String> score4UniqueTokens = new TreeSet<String>();

		TreeMap<String, TreeMap<String, Integer>> uniqueScore1keywordsInTitle = new TreeMap<String, TreeMap<String, Integer>>();
		TreeMap<String, TreeMap<String, Integer>> uniqueScore2keywordsInTitle = new TreeMap<String, TreeMap<String, Integer>>();
		TreeMap<String, TreeMap<String, Integer>> uniqueScore3keywordsInTitle = new TreeMap<String, TreeMap<String, Integer>>();
		TreeMap<String, TreeMap<String, Integer>> uniqueScore4keywordsInTitle = new TreeMap<String, TreeMap<String, Integer>>();

		TreeMap<String, TreeMap<String, Integer>> score1keywordsInTitle = new TreeMap<String, TreeMap<String, Integer>>();
		TreeMap<String, TreeMap<String, Integer>> score2keywordsInTitle = new TreeMap<String, TreeMap<String, Integer>>();
		TreeMap<String, TreeMap<String, Integer>> score3keywordsInTitle = new TreeMap<String, TreeMap<String, Integer>>();
		TreeMap<String, TreeMap<String, Integer>> score4keywordsInTitle = new TreeMap<String, TreeMap<String, Integer>>();

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

				// preprocessing (all ready cleaned)
				String cleanProductTitle = productTitle;
				String cleanProductDesc = productDesc;

				List<String> titleTokens = getTermsAsListByLucene(cleanProductTitle);
				List<String> descTokens = getTermsAsListByLucene(cleanProductDesc);

				if (!uniqueScore1keywordsInTitle.containsKey(query))
					uniqueScore1keywordsInTitle.put(query,
							new TreeMap<String, Integer>());
				if (!uniqueScore2keywordsInTitle.containsKey(query))
					uniqueScore2keywordsInTitle.put(query,
							new TreeMap<String, Integer>());
				if (!uniqueScore3keywordsInTitle.containsKey(query))
					uniqueScore3keywordsInTitle.put(query,
							new TreeMap<String, Integer>());
				if (!uniqueScore4keywordsInTitle.containsKey(query))
					uniqueScore4keywordsInTitle.put(query,
							new TreeMap<String, Integer>());

				if (!score1keywordsInTitle.containsKey(query))
					score1keywordsInTitle.put(query,
							new TreeMap<String, Integer>());
				if (!score2keywordsInTitle.containsKey(query))
					score2keywordsInTitle.put(query,
							new TreeMap<String, Integer>());
				if (!score3keywordsInTitle.containsKey(query))
					score3keywordsInTitle.put(query,
							new TreeMap<String, Integer>());
				if (!score4keywordsInTitle.containsKey(query))
					score4keywordsInTitle.put(query,
							new TreeMap<String, Integer>());

				if (medianRelevance == 1) {
					for (String k : titleTokens) {
						if (!score1keywordsInTitle.get(query).containsKey(k))
							score1keywordsInTitle.get(query).put(k, 1);
						else
							score1keywordsInTitle.get(query)
									.put(k,
											score1keywordsInTitle.get(query)
													.get(k) + 1);
					}
				} else if (medianRelevance == 2) {
					for (String k : titleTokens) {
						if (!score2keywordsInTitle.get(query).containsKey(k))
							score2keywordsInTitle.get(query).put(k, 1);
						else
							score2keywordsInTitle.get(query)
									.put(k,
											score2keywordsInTitle.get(query)
													.get(k) + 1);
					}
				} else if (medianRelevance == 3) {
					for (String k : titleTokens) {
						if (!score3keywordsInTitle.get(query).containsKey(k))
							score3keywordsInTitle.get(query).put(k, 1);
						else
							score3keywordsInTitle.get(query)
									.put(k,
											score3keywordsInTitle.get(query)
													.get(k) + 1);
					}
				} else if (medianRelevance == 4) {
					for (String k : titleTokens) {
						if (!score4keywordsInTitle.get(query).containsKey(k))
							score4keywordsInTitle.get(query).put(k, 1);
						else
							score4keywordsInTitle.get(query)
									.put(k,
											score4keywordsInTitle.get(query)
													.get(k) + 1);
					}
				} else
					throw new Exception("Impossible!!!");

				// List<String> titleTokens = getNgramTermsAsListByLucene(
				// cleanProductTitle, 2, 3, true, false);
				// List<String> descTokens = getNgramTermsAsListByLucene(
				// cleanProductDesc, 2, 3, true, false);

				// System.out.println("[id=" + id + "]");
				// System.out.println("query:" + query);
				// System.out.println("product_title:" + productTitle);
				// System.out.println("product_description:" + productDesc);
				// System.out.println("relevance_variance:" +
				// relevance_variance);
				// System.out.println("median_relevance:" + medianRelevance);

				count++;
			}

			System.out.println("\nTotal train records: " + count);

		} finally {
			trainIn.close();
		}

		// -------------------------------------------------------------------------------------------
		// Test Data

		TreeMap<String, List<String>> titleTokensByQueryInTest = new TreeMap<String, List<String>>();

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

				// preprocessing (all ready cleaned)
				String cleanProductTitle = productTitle;
				String cleanProductDesc = productDesc;

				List<String> titleTokens = getTermsAsListByLucene(cleanProductTitle);
				List<String> descTokens = getTermsAsListByLucene(cleanProductDesc);

				if (!titleTokensByQueryInTest.containsKey(query)) {
					titleTokensByQueryInTest.put(query, new ArrayList<String>(
							titleTokens));
				} else {
					List<String> currentTitleTokens = titleTokensByQueryInTest
							.get(query);
					for (String t : titleTokens) {
						if (!currentTitleTokens.contains(t))
							currentTitleTokens.add(t);
					}

					titleTokensByQueryInTest.put(query, currentTitleTokens);
				}

				count++;
			}

			System.out.println("Total test records: " + count);

		} finally {
			testIn.close();
		}

		for (String query : score1keywordsInTitle.keySet()) {
			TreeMap<String, Integer> keywords = score1keywordsInTitle
					.get(query);

			List<String> testTitleTokens = titleTokensByQueryInTest.get(query);

			for (String k : keywords.keySet()) {
				if (testTitleTokens.contains(k)
				// && !score2keywordsInTitle.get(query).containsKey(k)
				// && !score3keywordsInTitle.get(query).containsKey(k)
				// && !score4keywordsInTitle.get(query).containsKey(k)
				) {
					// if (score1keywordsInTitle.get(query).get(k) >= 2)
					if (k.length() > 1) {
						uniqueScore1keywordsInTitle.get(query).put(k,
								score1keywordsInTitle.get(query).get(k));

						if (!score1UniqueTokens.contains(k))
							score1UniqueTokens.add(k);

						if (!allUniqueTokens.contains(k))
							allUniqueTokens.add(k);
					}
				}
			}
		}

		for (String query : score2keywordsInTitle.keySet()) {
			TreeMap<String, Integer> keywords = score2keywordsInTitle
					.get(query);

			List<String> testTitleTokens = titleTokensByQueryInTest.get(query);

			for (String k : keywords.keySet()) {
				if (testTitleTokens.contains(k)
				// && !score1keywordsInTitle.get(query).containsKey(k)
				// && !score3keywordsInTitle.get(query).containsKey(k)
				// && !score4keywordsInTitle.get(query).containsKey(k)
				) {
					// if (score1keywordsInTitle.get(query).get(k) >= 2)
					if (k.length() > 1) {
						uniqueScore2keywordsInTitle.get(query).put(k,
								score2keywordsInTitle.get(query).get(k));

						if (!score2UniqueTokens.contains(k))
							score2UniqueTokens.add(k);

						if (!allUniqueTokens.contains(k))
							allUniqueTokens.add(k);
					}
				}
			}
		}

		for (String query : score3keywordsInTitle.keySet()) {
			TreeMap<String, Integer> keywords = score3keywordsInTitle
					.get(query);

			List<String> testTitleTokens = titleTokensByQueryInTest.get(query);

			for (String k : keywords.keySet()) {
				if (testTitleTokens.contains(k)
				// && !score1keywordsInTitle.get(query).containsKey(k)
				// && !score2keywordsInTitle.get(query).containsKey(k)
				// && !score4keywordsInTitle.get(query).containsKey(k)
				) {
					// if (score1keywordsInTitle.get(query).get(k) >= 2)
					if (k.length() > 1) {
						uniqueScore3keywordsInTitle.get(query).put(k,
								score3keywordsInTitle.get(query).get(k));

						if (!score3UniqueTokens.contains(k))
							score3UniqueTokens.add(k);

						if (!allUniqueTokens.contains(k))
							allUniqueTokens.add(k);
					}
				}
			}
		}

		for (String query : score4keywordsInTitle.keySet()) {
			TreeMap<String, Integer> keywords = score4keywordsInTitle
					.get(query);

			List<String> testTitleTokens = titleTokensByQueryInTest.get(query);

			for (String k : keywords.keySet()) {
				if (testTitleTokens.contains(k)
				// && !score1keywordsInTitle.get(query).containsKey(k)
				// && !score2keywordsInTitle.get(query).containsKey(k)
				// && !score3keywordsInTitle.get(query).containsKey(k)
				) {
					// if (score1keywordsInTitle.get(query).get(k) >= 2)
					if (k.length() > 1) {
						uniqueScore4keywordsInTitle.get(query).put(k,
								score4keywordsInTitle.get(query).get(k));

						if (!score4UniqueTokens.contains(k))
							score4UniqueTokens.add(k);

						if (!allUniqueTokens.contains(k))
							allUniqueTokens.add(k);
					}
				}
			}
		}

		if (uniqueScore1keywordsInTitle.size() > 0) {
			System.out
					.println("[Score 1 Unique Keywords, exists in test dataset too]");
			for (String q : uniqueScore1keywordsInTitle.keySet()) {
				System.out.println("\n[Query=" + q + "]");
				TreeMap<String, Integer> keywords = uniqueScore1keywordsInTitle
						.get(q);
				for (String k : keywords.keySet()) {
					System.out.println(k + " (" + keywords.get(k) + " times)");
				}
			}

			System.out.println("Total unique score-1 token in title: "
					+ score1UniqueTokens.size());

		}

		if (uniqueScore2keywordsInTitle.size() > 0) {
			System.out
					.println("[Score 2 Unique Keywords, exists in test dataset too]");
			for (String q : uniqueScore2keywordsInTitle.keySet()) {
				System.out.println("\n[Query=" + q + "]");
				TreeMap<String, Integer> keywords = uniqueScore2keywordsInTitle
						.get(q);
				for (String k : keywords.keySet()) {
					System.out.println(k + " (" + keywords.get(k) + " times)");
				}
			}

			System.out.println("Total unique score-2 token in title: "
					+ score2UniqueTokens.size());

		}

		if (uniqueScore3keywordsInTitle.size() > 0) {
			System.out
					.println("[Score 3 Unique Keywords, exists in test dataset too]");
			for (String q : uniqueScore3keywordsInTitle.keySet()) {
				System.out.println("\n[Query=" + q + "]");
				TreeMap<String, Integer> keywords = uniqueScore3keywordsInTitle
						.get(q);
				for (String k : keywords.keySet()) {
					System.out.println(k + " (" + keywords.get(k) + " times)");
				}
			}

			System.out.println("Total unique score-3 token in title: "
					+ score3UniqueTokens.size());

		}

		if (uniqueScore4keywordsInTitle.size() > 0) {
			System.out
					.println("[Score 4 Unique Keywords, exists in test dataset too]");
			for (String q : uniqueScore4keywordsInTitle.keySet()) {
				System.out.println("\n[Query=" + q + "]");
				TreeMap<String, Integer> keywords = uniqueScore4keywordsInTitle
						.get(q);
				for (String k : keywords.keySet()) {
					System.out.println(k + " (" + keywords.get(k) + " times)");
				}
			}

			System.out.println("Total unique score-4 token in title: "
					+ score4UniqueTokens.size());

		}

		// write to file
		BufferedWriter featureOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFeatureFile, false), "UTF-8"));
		for (String k : allUniqueTokens)
			featureOut.append(k + newLine);
		featureOut.flush();
		featureOut.close();

		System.out.println("Total unique all token in title: "
				+ allUniqueTokens.size());

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

	private List<String> getTermsAsListByLucene(String text) throws IOException {
		return getTermsAsListByLucene(text, true, false);
	}

	private List<String> getTermsAsListByLucene(String text, boolean english,
			boolean digits) throws IOException {
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

					boolean valid = false;
					if (english && !digits)
						valid = isAllEnglish(word);
					else if (!english && digits)
						valid = isAllDigits(word);
					else if (english && digits)
						valid = isAllEnglishAndDigits(word);

					if (valid) {
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

	private List<String> getNgramTermsAsListByLucene(String text, int ngram)
			throws IOException {
		return getNgramTermsAsListByLucene(text, ngram, 1, true, true);
	}

	private List<String> getNgramTermsAsListByLucene(String text, int ngram,
			int minTokenLen, boolean english, boolean digits)
			throws IOException {
		List<String> result = new ArrayList<String>();

		Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
				.getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
		// ts = new PorterStemFilter(ts);
		ts = new ShingleFilter(ts, ngram, ngram);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			here: while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					String[] tokens = word.split("\\s");
					if (tokens.length == ngram) {

						for (String token : tokens) {
							if (token.length() < minTokenLen)
								continue here;
						}

						boolean valid = false;

						if (english && !digits)
							valid = isAllEnglish(word);
						else if (!english && digits)
							valid = isAllDigits(word);
						else if (english && digits)
							valid = isAllEnglishAndDigits(word);

						if (valid) {
							if (!result.contains(word))
								result.add(word);
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

		String[] tokens = text.split("\\s");

		for (String token : tokens) {
			for (int i = 0; i < token.length(); i++) {
				char c = token.charAt(i);
				if (!Character.isAlphabetic(c) && c != '-') {
					result = false;
					break;
				}
			}
		}

		return result;
	}

	public boolean isAllDigits(String text) {
		boolean result = true;

		String[] tokens = text.split("\\s");

		for (String token : tokens) {
			for (int i = 0; i < token.length(); i++) {
				char c = token.charAt(i);
				if (!Character.isDigit(c)) {
					result = false;
					break;
				}
			}
		}

		return result;
	}

	public boolean isAllEnglishAndDigits(String text) {
		boolean result = true;

		String[] tokens = text.split("\\s");

		for (String token : tokens) {
			for (int i = 0; i < token.length(); i++) {
				char c = token.charAt(i);
				if (!Character.isAlphabetic(c) && c != '-'
						&& !Character.isDigit(c)) {
					result = false;
					break;
				}
			}
		}

		return result;
	}

	public static void main(String[] args) throws Exception {
		args = new String[5];
		// args[0] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_porter_stem_compound_markpeng_20150606.csv";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/test_filterred_porter_stem_compound_markpeng_20150606.csv";
		args[0] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_stem_compound_newfeature_markpeng_20150530.csv";
		args[1] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test_filterred_stem_compound_newfeature_markpeng_20150530.csv";
		// args[0] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/train.csv";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/test.csv";
		args[2] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_markpeng_20150601.csv";
		args[3] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test_filterred_markpeng_20150601.csv";
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
		// worker.extractCompoundFromTitleAndDescription(trainFile, testFile,
		// outputTrain, outputTest);
		// String outputFile =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/distinct_query_terms_20150518.txt";
		// worker.extractAllTermsInQuery(trainFile, testFile, outputFile);
		// worker.extractBigramWithDigitFromQuery(trainFile, testFile,
		// outputTrain, outputTest);
		// worker.extractBigramTopicCompoundFromTitleMappingInDescription(
		// trainFile, testFile, outputTrain, outputTest);
		// worker.extractSmallerWordsBaseOnQuery(trainFile, testFile,
		// outputTrain,
		// outputTest);
		// worker.extractKeywordFromTitleAndDescriptionByScore(
		// trainFile,
		// testFile,
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/unique_score_keywords_20150602.txt");
		// worker.extractKeywordFromTitleAndDescriptionByScore(
		// trainFile,
		// testFile,
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/unique_score_keywords_noexclude_20150606.txt");
		// worker.extractDistinctQuery("/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_stem_compound_markpeng.csv");
		worker.extractDistinctQuery("/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_crawl_markpeng_20150615.csv");
	}
}

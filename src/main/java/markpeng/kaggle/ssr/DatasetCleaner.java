package markpeng.kaggle.ssr;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class DatasetCleaner {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public List<String> readFile(String filePath) throws Exception {
		List<String> result = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				if (!result.contains(tmp))
					result.add(tmp);
			}
		} finally {
			in.close();
		}

		return result;
	}

	public Set<String> readDictionary(String dicPath) throws Exception {
		TreeSet<String> result = new TreeSet<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(dicPath), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				if (!result.contains(tmp) && !tmp.startsWith("'"))
					result.add(tmp);
			}
		} finally {
			in.close();
		}

		return result;
	}

	public List<String> readFeature(String... featureFiles) throws Exception {
		List<String> features = new ArrayList<String>();

		for (String featureFile : featureFiles) {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(featureFile), "UTF-8"));

			try {
				String aLine = null;
				while ((aLine = in.readLine()) != null) {
					String tmp = aLine.toLowerCase().trim();
					String[] tmpArray = tmp.split("\\s");
					String feature = tmpArray[0].trim();
					if (!features.contains(feature))
						features.add(feature);
				}
			} finally {
				in.close();
			}
		}

		return features;
	}

	public void clean(String trainFile, String testFile, String outputTrain,
			String outputTest, String compoundPath) throws Exception {

		List<String> compounds = readFile(compoundPath);

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

				// replace compounds
				for (String c : compounds) {
					String tmp = c.replace(" ", "");
					if (cleanQuery.contains(tmp))
						cleanQuery = cleanQuery.replace(tmp, c);
					if (cleanProductTitle.contains(tmp))
						cleanProductTitle = cleanProductTitle.replace(tmp, c);
					if (cleanProductDesc.contains(tmp))
						cleanProductDesc = cleanProductDesc.replace(tmp, c);
				}

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

				// replace compounds
				for (String c : compounds) {
					String tmp = c.replace(" ", "");
					if (cleanQuery.contains(tmp))
						cleanQuery = cleanQuery.replace(tmp, c);
					if (cleanProductTitle.contains(tmp))
						cleanProductTitle = cleanProductTitle.replace(tmp, c);
					if (cleanProductDesc.contains(tmp))
						cleanProductDesc = cleanProductDesc.replace(tmp, c);
				}

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

	public void generate(String trainFile, String testFile, String outputTrain,
			String outputTest, String compoundPath) throws Exception {

		List<String> compounds = readFile(compoundPath);

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
						+ "median_relevance\",\"relevance_variance\",\""
						+ "qInTitle\",\"qInDesc\",\""
						+ "prefixMatchInTitle\",\"secondMatchInTitle\",\"midMatchInTitle\",\"suffixMatchInTitle\"");
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

				// replace compounds
				for (String c : compounds) {
					String tmp = c.replace(" ", "");
					if (cleanQuery.contains(tmp))
						cleanQuery = cleanQuery.replace(tmp, c);
					if (cleanProductTitle.contains(tmp))
						cleanProductTitle = cleanProductTitle.replace(tmp, c);
					if (cleanProductDesc.contains(tmp))
						cleanProductDesc = cleanProductDesc.replace(tmp, c);
				}

				Hashtable<String, Integer> qTokens = getTermFreqByLucene(
						cleanQuery, true, true);
				Hashtable<String, Integer> titleTokens = getTermFreqByLucene(
						cleanProductTitle, true, true);
				Hashtable<String, Integer> descTokens = getTermFreqByLucene(
						cleanProductDesc, true, true);

				Hashtable<String, Integer> matchedTermsInTitle = new Hashtable<String, Integer>();
				Hashtable<String, Integer> matchedTermsInDesc = new Hashtable<String, Integer>();
				int titleMatched = 0;
				int descMatched = 0;
				for (String q : qTokens.keySet()) {
					if (titleTokens.containsKey(q)) {
						if (!matchedTermsInTitle.containsKey(query))
							matchedTermsInTitle.put(query, titleTokens.get(q));
						else
							matchedTermsInTitle.put(query,
									matchedTermsInTitle.get(query)
											+ titleTokens.get(q));

						titleMatched++;
					}
					if (descTokens.containsKey(q)) {
						if (!matchedTermsInDesc.containsKey(query))
							matchedTermsInDesc.put(query, descTokens.get(q));
						else
							matchedTermsInDesc.put(
									query,
									matchedTermsInDesc.get(query)
											+ descTokens.get(q));

						descMatched++;
					}
				}

				// qInTitle
				double qInTitle = (double) titleMatched / qTokens.size();

				// qInDesc
				double qInDesc = (double) descMatched / qTokens.size();

				String[] qTmp = cleanQuery.split("\\s");

				// prefixMatch in title
				int prefixMatchInTitle = 0;
				String prefixQ = qTmp[0];
				if (titleTokens.containsKey(prefixQ))
					prefixMatchInTitle = 1;

				// TODO:
				// prefix match in 1st token of title
				// 2nd match in 2nd token of title

				// secondMatch in title
				int secondMatchInTitle = 0;
				if (qTmp.length >= 2) {
					String secondQ = qTmp[1];
					if (titleTokens.containsKey(secondQ))
						secondMatchInTitle = 1;
				}

				// midMatch in title
				int midMatchInTitle = 0;
				for (int i = 1; i < qTmp.length - 1; i++) {
					if (titleTokens.containsKey(qTmp[i]))
						midMatchInTitle = 1;
				}

				// suffixMatch in title
				int suffixMatchInTitle = 0;
				String suffixQ = qTmp[qTmp.length - 1];
				if (titleTokens.containsKey(suffixQ))
					suffixMatchInTitle = 1;

				resultStr.append("\"" + id + "\",\"" + cleanQuery + "\",\""
						+ cleanProductTitle + "\",\"" + cleanProductDesc
						+ "\",\"" + medianRelevance + "\",\""
						+ relevance_variance + "\",\"" + qInTitle + "\",\""
						+ qInDesc + "\",\"" + prefixMatchInTitle + "\",\""
						+ secondMatchInTitle + "\",\"" + midMatchInTitle
						+ "\",\"" + suffixMatchInTitle + "\"");
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
				.append("\"id\",\"query\",\"product_title\",\"product_description\",\""
						+ "qInTitle\",\"qInDesc\",\""
						+ "prefixMatchInTitle\",\"secondMatchInTitle\",\"midMatchInTitle\",\"suffixMatchInTitle\"");
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

				// replace compounds
				for (String c : compounds) {
					String tmp = c.replace(" ", "");
					if (cleanQuery.contains(tmp))
						cleanQuery = cleanQuery.replace(tmp, c);
					if (cleanProductTitle.contains(tmp))
						cleanProductTitle = cleanProductTitle.replace(tmp, c);
					if (cleanProductDesc.contains(tmp))
						cleanProductDesc = cleanProductDesc.replace(tmp, c);
				}

				Hashtable<String, Integer> qTokens = getTermFreqByLucene(
						cleanQuery, true, true);
				Hashtable<String, Integer> titleTokens = getTermFreqByLucene(
						cleanProductTitle, true, true);
				Hashtable<String, Integer> descTokens = getTermFreqByLucene(
						cleanProductDesc, true, true);

				Hashtable<String, Integer> matchedTermsInTitle = new Hashtable<String, Integer>();
				Hashtable<String, Integer> matchedTermsInDesc = new Hashtable<String, Integer>();
				int titleMatched = 0;
				int descMatched = 0;
				for (String q : qTokens.keySet()) {
					if (titleTokens.containsKey(q)) {
						if (!matchedTermsInTitle.containsKey(query))
							matchedTermsInTitle.put(query, titleTokens.get(q));
						else
							matchedTermsInTitle.put(query,
									matchedTermsInTitle.get(query)
											+ titleTokens.get(q));

						titleMatched++;
					}
					if (descTokens.containsKey(q)) {
						if (!matchedTermsInDesc.containsKey(query))
							matchedTermsInDesc.put(query, descTokens.get(q));
						else
							matchedTermsInDesc.put(
									query,
									matchedTermsInDesc.get(query)
											+ descTokens.get(q));

						descMatched++;
					}
				}

				// qInTitle
				double qInTitle = (double) titleMatched / qTokens.size();

				// qInDesc
				double qInDesc = (double) descMatched / qTokens.size();

				String[] qTmp = cleanQuery.split("\\s");

				// prefixMatch in title
				int prefixMatchInTitle = 0;
				String prefixQ = qTmp[0];
				if (titleTokens.containsKey(prefixQ))
					prefixMatchInTitle = 1;

				// secondMatch in title
				int secondMatchInTitle = 0;
				if (qTmp.length >= 2) {
					String secondQ = qTmp[1];
					if (titleTokens.containsKey(secondQ))
						secondMatchInTitle = 1;
				}

				// midMatch in title
				int midMatchInTitle = 0;
				for (int i = 1; i < qTmp.length - 1; i++) {
					if (titleTokens.containsKey(qTmp[i]))
						midMatchInTitle = 1;
				}

				// suffixMatch in title
				int suffixMatchInTitle = 0;
				String suffixQ = qTmp[qTmp.length - 1];
				if (titleTokens.containsKey(suffixQ))
					suffixMatchInTitle = 1;

				resultStr.append("\"" + id + "\",\"" + cleanQuery + "\",\""
						+ cleanProductTitle + "\",\"" + cleanProductDesc
						+ "\",\"" + qInTitle + "\",\"" + qInDesc + "\",\""
						+ prefixMatchInTitle + "\",\"" + secondMatchInTitle
						+ "\",\"" + midMatchInTitle + "\",\""
						+ suffixMatchInTitle + "\"");
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

	public void run(String trainFile, String testFile, String outputTrain,
			String outputTest) throws Exception {
		System.setOut(new PrintStream(
				new BufferedOutputStream(
						new FileOutputStream(
								"/home/markpeng/Share/Kaggle/Search Results Relevance/preprocess_notmatched_all_20150514.txt")),
				true));
		// System.setOut(new PrintStream(
		// new BufferedOutputStream(
		// new FileOutputStream(
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/preprocess_notmatched_train.txt")),
		// true));

		// Set<String> dictionary = readDictionary(dicPath);

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

			// for (String[] tokens : allRows) {
			int matched = 0;
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

				Hashtable<String, Integer> qTokens = getTermFreqByLucene(cleanQuery);
				Hashtable<String, Integer> titleTokens = getTermFreqByLucene(cleanProductTitle);
				Hashtable<String, Integer> descTokens = getTermFreqByLucene(cleanProductDesc);

				Hashtable<String, Integer> matchedTermsInTitle = new Hashtable<String, Integer>();
				Hashtable<String, Integer> matchedTermsInDesc = new Hashtable<String, Integer>();
				for (String q : qTokens.keySet()) {
					if (titleTokens.containsKey(q)) {
						matchedTermsInTitle.put(q, titleTokens.get(q));
					}
					if (descTokens.containsKey(q)) {
						matchedTermsInDesc.put(q, descTokens.get(q));
					}
				}

				if (matchedTermsInTitle.size() > 0
						|| matchedTermsInDesc.size() > 0) {
					// System.out.println("[id=" + id + "]");
					// System.out.println("query:" + cleanQuery);
					// System.out.println("product_title:" + cleanProductTitle);
					// System.out.println("product_description:"
					// + cleanProductDesc);
					// System.out.println("median_relevance:" +
					// medianRelevance);
					// System.out.println("relevance_variance:"
					// + relevance_variance);
					// System.out.println("matched query terms in title:"
					// + matchedTermsInTitle.toString());
					// System.out.println("matched query terms in description:"
					// + matchedTermsInDesc.toString());
					// System.out.println("\n");
					// System.out.println();

					matched++;
				} else {
					System.out.println("[id=" + id + "]");
					System.out.println("query:" + cleanQuery);
					System.out.println("product_title:" + cleanProductTitle);
					System.out.println("product_description:"
							+ cleanProductDesc);
					System.out.println("median_relevance:" + medianRelevance);
					System.out.println("relevance_variance:"
							+ relevance_variance);
					System.out.println("matched query terms in title:"
							+ matchedTermsInTitle.toString());
					System.out.println("matched query terms in description:"
							+ matchedTermsInDesc.toString());
					System.out.println("\n");
					// System.out.println();

				}

				count++;
			}

			System.out.println("Total train records: " + count);
			System.out
					.println("Total query-matched records in title or description: "
							+ matched);
			System.out
					.println("Total not-matched records in title or description: "
							+ (count - matched));

		} finally {
			trainIn.close();
		}

		// -------------------------------------------------------------------------------------------
		// Test Data

		try {
			System.out
					.println("\n\n// -------------------------------------------------------------------------------------------\n\n");
			System.out.println("Test Data");

			// creates a CSV parser
			CsvParser testParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			testParser.beginParsing(testIn);

			int matched = 0;
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

				Hashtable<String, Integer> qTokens = getTermFreqByLucene(cleanQuery);
				Hashtable<String, Integer> titleTokens = getTermFreqByLucene(cleanProductTitle);
				Hashtable<String, Integer> descTokens = getTermFreqByLucene(cleanProductDesc);

				Hashtable<String, Integer> matchedTermsInTitle = new Hashtable<String, Integer>();
				Hashtable<String, Integer> matchedTermsInDesc = new Hashtable<String, Integer>();
				for (String q : qTokens.keySet()) {
					if (titleTokens.containsKey(q)) {
						matchedTermsInTitle.put(q, titleTokens.get(q));
					}
					if (descTokens.containsKey(q)) {
						matchedTermsInDesc.put(q, descTokens.get(q));
					}
				}

				if (matchedTermsInTitle.size() > 0
						|| matchedTermsInDesc.size() > 0) {
					// System.out.println("[id=" + id + "]");
					// System.out.println("query:" + cleanQuery);
					// System.out.println("product_title:" + cleanProductTitle);
					// System.out.println("product_description:"
					// + cleanProductDesc);
					// System.out.println("median_relevance:" +
					// medianRelevance);
					// System.out.println("relevance_variance:"
					// + relevance_variance);
					// System.out.println("matched query terms in title:"
					// + matchedTermsInTitle.toString());
					// System.out.println("matched query terms in description:"
					// + matchedTermsInDesc.toString());
					// System.out.println("\n");
					// System.out.println();

					matched++;
				} else {
					System.out.println("[id=" + id + "]");
					System.out.println("query:" + cleanQuery);
					System.out.println("product_title:" + cleanProductTitle);
					System.out.println("product_description:"
							+ cleanProductDesc);
					System.out.println("matched query terms in title:"
							+ matchedTermsInTitle.toString());
					System.out.println("matched query terms in description:"
							+ matchedTermsInDesc.toString());
					System.out.println("\n");
					// System.out.println();

				}

				count++;
			}

			System.out.println("Total test records: " + count);
			System.out
					.println("Total query-matched records in title or description: "
							+ matched);
			System.out
					.println("Total not-matched records in title or description: "
							+ (count - matched));

		} finally {
			testIn.close();
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
		return getTermFreqByLucene(text, true, false);
	}

	private Hashtable<String, Integer> getTermFreqByLucene(String text,
			boolean english, boolean digits) throws IOException {
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

					boolean valid = false;
					if (english && !digits)
						valid = isAllEnglish(word);
					else if (!english && digits)
						valid = isAllDigits(word);
					else if (english && digits)
						valid = isAllEnglishAndDigits(word);

					if (valid) {
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

	public boolean isAllDigits(String text) {
		boolean result = true;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!Character.isDigit(c)) {
				result = false;
				break;
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

	public SpellChecker loadDictionary(String indexPath, String dicPath) {
		SpellChecker spellChecker = null;

		try {
			File dir = new File(indexPath);
			Directory directory = FSDirectory.open(dir);
			spellChecker = new SpellChecker(directory);
			PlainTextDictionary dictionary = new PlainTextDictionary(new File(
					dicPath));
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
					analyzer);
			spellChecker.indexDictionary(dictionary, config, false);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return spellChecker;
	}

	public static void main(String[] args) throws Exception {
		args = new String[5];
		args[0] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train.csv";
		args[1] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test.csv";
		args[2] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_stem_compound.csv";
		args[3] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test_filterred_stem_compound.csv";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_markpeng.csv";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/test_filterred_markpeng.csv";
		// args[4] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/JOrtho/dictionary_en_2015_05/IncludedWords.txt";
		args[4] = "/home/markpeng/Share/Kaggle/Search Results Relevance/english-compound-words.txt";

		if (args.length < 5) {
			System.out
					.println("Arguments: [train.csv] [test.csv] [output train] [output test] [compound path]");
			return;
		}
		String trainFile = args[0];
		String testFile = args[1];
		// String[] featureFiles = args[2].split("\\|");
		String outputTrain = args[2];
		String outputTest = args[3];
		String compoundPath = args[4];

		DatasetCleaner worker = new DatasetCleaner();
		// worker.generate(trainFile, testFile, outputTrain, outputTest);
		worker.clean(trainFile, testFile, outputTrain, outputTest, compoundPath);
		// worker.run(trainFile, testFile, outputTrain, outputTest);

		// String testQ = "refrigir";
		// String testP = "refriger";
		// // String testQ = "fridge";
		// // String testP = "Refrigerator";
		// String indexPath =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/spellCheckerIndex";
		// String dicPath =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/JOrtho/dictionary_en_2015_05/IncludedWords.txt";
		// SpellChecker checker = worker.loadDictionary(indexPath, dicPath);
		// String[] suggestions = checker.suggestSimilar(testQ, 5);
		// checker.setAccuracy((float) 0.7);
		// // best measure =>
		// // http://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance
		// checker.setStringDistance(new JaroWinklerDistance());
		// // checker.setStringDistance(new NGramDistance(7));
		// // checker.setStringDistance(new LevensteinDistance());
		// System.out.println("Minimum accurarcy: " + checker.getAccuracy());
		// System.out.println(checker.getStringDistance().toString());
		// System.out.println(checker.getStringDistance()
		// .getDistance(testQ, testP));
		//
		// if (suggestions.length > 0) {
		// System.out.println(testQ + " correction: "
		// + Arrays.asList(suggestions));
		// }
		// suggestions = checker.suggestSimilar(testP, 5);
		// if (suggestions.length > 0) {
		// System.out.println(testP + " correction: "
		// + Arrays.asList(suggestions));
		// }

		// WordBreakSpellChecker breakChecker = new WordBreakSpellChecker();
		// breakChecker.suggestWordBreaks(term, maxSuggestions, ir, suggestMode,
		// sortMethod)
	}
}

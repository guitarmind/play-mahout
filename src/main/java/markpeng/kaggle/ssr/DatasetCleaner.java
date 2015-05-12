package markpeng.kaggle.ssr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
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

public class DatasetCleaner {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

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

	public void run(String trainFile, String testFile, String outputTrain,
			String outputTest) throws Exception {
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

		try {
			// creates a CSV parser
			CsvParser trainParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			trainParser.beginParsing(new FileReader(trainFile));

			// for (String[] tokens : allRows) {
			int count = 0;
			String[] tokens;
			while ((tokens = trainParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();
				double medianRelevance = Double.parseDouble(tokens[4]);
				double relevance_variance = Double.parseDouble(tokens[5]);
				// String id = record.get("id");
				// String query = record.get("query");
				// String productTitle = record.get("product_title");
				// String productDesc = record.get("product_description");
				// double medianRelevance = Double.parseDouble(record
				// .get("medianRelevance"));
				// double relevance_variance = Double.parseDouble(record
				// .get("relevance_variance"));

				// preprocessing
				String cleanQuery = getTextFromRawData(query);
				String cleanProductTitle = getTextFromRawData(productTitle);
				String cleanProductDesc = getTextFromRawData(productDesc);

				System.out.println("[id=" + id + "]");
				System.out.println("query:" + cleanQuery);
				System.out.println("product_title:" + cleanProductTitle);
				System.out.println("product_description:" + cleanProductDesc);
				System.out.println("median_relevance:" + medianRelevance);
				System.out.println("relevance_variance:" + relevance_variance);
				System.out.println("\n\n\n\n");
				// System.out.println();

				count++;
			}

			System.out.println("Total train records: " + count);

			// String aLine = null;
			// // skip header
			// trainIn.readLine();
			// while ((aLine = trainIn.readLine()) != null) {
			// String tmp = aLine.toLowerCase().trim();
			// String[] tokens = tmp.split(",^\\S+");
			// String id = tokens[0];
			// String query = tokens[1].replace("\"", "").trim();
			// String productTitle = tokens[2].replace("\"", "").trim();
			// String productDesc = tokens[3].replace("\"", "").trim();
			// double medianRelevance = Double.parseDouble(tokens[4]);
			// double relevance_variance = Double.parseDouble(tokens[5]);
			//
			// // preprocessing
			// String cleanQuery = getTextFromRawData(query);
			// String cleanProductTitle = getTextFromRawData(productTitle);
			// String cleanProductDesc = getTextFromRawData(productDesc);
			//
			// System.out.println("[id=" + id + "]");
			// System.out.println("query:" + cleanQuery);
			// System.out.println("product_title:" + cleanProductTitle);
			// System.out.println("product_description:" + cleanProductDesc);
			// System.out.println("median_relevance:" + medianRelevance);
			// System.out.println("relevance_variance:" + relevance_variance);
			// System.out.println("\n\n\n\n");
			// // System.out.println();
			//
			// if (resultStr.length() >= BUFFER_LENGTH) {
			// trainOut.write(resultStr.toString());
			// trainOut.flush();
			// resultStr.setLength(0);
			// }
			//
			// } // end of train file loop

		} finally {
			trainIn.close();

			trainOut.write(resultStr.toString());
			trainOut.flush();
			trainOut.close();
			resultStr.setLength(0);
		}

		// -------------------------------------------------------------------------------------------
		// Test Data

		try {

		} finally {
			testIn.close();

			testOut.write(resultStr.toString());
			testOut.flush();
			testOut.close();
			resultStr.setLength(0);
		}
	}

	public String getTextFromRawData(String raw) {
		String result = raw;

		Document doc = Jsoup.parse(raw);
		Element body = doc.body();
		String plainText = body.text().trim();
		if (plainText.length() > 2) {
			System.out.println(plainText);
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
		ts = new PorterStemFilter(ts);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					if (result.get(word) == null)
						result.put(word, 1);
					else {
						result.put(word, result.get(word) + 1);
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

	public static void main(String[] args) throws Exception {
		args = new String[4];
		args[0] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train.csv";
		args[1] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test.csv";
		args[2] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred.csv";
		args[3] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test_filterred.csv";

		if (args.length < 4) {
			System.out
					.println("Arguments: [train.csv] [test.csv] [output train] [output test]");
			return;
		}
		String trainFile = args[0];
		String testFile = args[1];
		// String[] featureFiles = args[2].split("\\|");
		String outputTrain = args[2];
		String outputTest = args[3];

		DatasetCleaner worker = new DatasetCleaner();
		worker.run(trainFile, testFile, outputTrain, outputTest);

	}

}

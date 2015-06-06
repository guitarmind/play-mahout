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
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.search.spell.NGramDistance;
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

public class WrongCaseViewer {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public void view(String file) throws Exception {

		System.setOut(new PrintStream(
				new BufferedOutputStream(
						new FileOutputStream(
								"/home/markpeng/Share/Kaggle/Search Results Relevance/wrong_case_xgb_4498features_d=60_20150605.txt")),
				true));

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), "UTF-8"));

		CsvParserSettings settings = new CsvParserSettings();
		settings.setMaxColumns(5000);
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
			String[] tokens;
			while ((tokens = trainParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();
				double relevance_variance = Double.parseDouble(tokens[4]);
				double qInTitle = Double.parseDouble(tokens[5]);
				double qInDesc = Double.parseDouble(tokens[6]);
				double prefixMatchInTitle = Double.parseDouble(tokens[7]);
				double secondMatchInTitle = Double.parseDouble(tokens[8]);
				double midMatchInTitle = Double.parseDouble(tokens[9]);
				double suffixMatchInTitle = Double.parseDouble(tokens[10]);
				int qSize = Integer.parseInt(tokens[11]);
				int titleSize = Integer.parseInt(tokens[12]);
				int descSize = Integer.parseInt(tokens[13]);
				double titleRatio = Double.parseDouble(tokens[14]);
				double descRatio = Double.parseDouble(tokens[15]);
				int prefixMatchIn1stTokenOfTitle = Integer.parseInt(tokens[16]);
				int secondMatchIn2ndTokenOfTitle = Integer.parseInt(tokens[17]);
				int suffixMatchInLastTokenOfTitle = Integer
						.parseInt(tokens[18]);
				int matchIn1stTokenOfTitle = Integer.parseInt(tokens[19]);
				int matchIn2ndTokenOfTitle = Integer.parseInt(tokens[20]);
				int matchInLastTokenOfTitle = Integer.parseInt(tokens[21]);
				int compoundMatchInTitlePrefix = Integer.parseInt(tokens[22]);
				int compoundMatchInTitleSuffix = Integer.parseInt(tokens[23]);
				// double qBigramDistanceInTitle =
				// Double.parseDouble(tokens[24]);
				// double qBigramDistanceInDesc =
				// Double.parseDouble(tokens[25]);

				int medianRelevance = Integer
						.parseInt(tokens[tokens.length - 6]);
				int predict = Integer.parseInt(tokens[tokens.length - 5]);
				double P1 = Double.parseDouble(tokens[tokens.length - 4]);
				double P2 = Double.parseDouble(tokens[tokens.length - 3]);
				double P3 = Double.parseDouble(tokens[tokens.length - 2]);
				double P4 = Double.parseDouble(tokens[tokens.length - 1]);

				System.out.println("[id=" + id + "]");
				System.out.println("query:" + query);
				System.out.println("product_title:" + productTitle);
				System.out.println("product_description:" + productDesc);
				System.out.println("relevance_variance:" + relevance_variance);
				System.out.println("qInTitle:" + qInTitle);
				System.out.println("qInDesc:" + qInDesc);
				System.out.println("prefixMatchInTitle:" + prefixMatchInTitle);
				System.out.println("secondMatchInTitle:" + secondMatchInTitle);
				System.out.println("midMatchInTitle:" + midMatchInTitle);
				System.out.println("suffixMatchInTitle:" + suffixMatchInTitle);
				System.out.println("qSize:" + qSize);
				System.out.println("titleSize:" + titleSize);
				System.out.println("descSize:" + descSize);
				System.out.println("titleRatio:" + titleRatio);
				System.out.println("descRatio:" + descRatio);
				System.out.println("prefixMatchIn1stTokenOfTitle:"
						+ prefixMatchIn1stTokenOfTitle);
				System.out.println("secondMatchIn2ndTokenOfTitle:"
						+ secondMatchIn2ndTokenOfTitle);
				System.out.println("suffixMatchInLastTokenOfTitle:"
						+ suffixMatchInLastTokenOfTitle);
				System.out.println("matchIn1stTokenOfTitle:"
						+ matchIn1stTokenOfTitle);
				System.out.println("matchIn2ndTokenOfTitle:"
						+ matchIn2ndTokenOfTitle);
				System.out.println("matchInLastTokenOfTitle:"
						+ matchInLastTokenOfTitle);
				System.out.println("compoundMatchInTitlePrefix:"
						+ compoundMatchInTitlePrefix);
				System.out.println("compoundMatchInTitleSuffix:"
						+ compoundMatchInTitleSuffix);
				// System.out.println("qBigramDistanceInTitle:"
				// + qBigramDistanceInTitle);
				// System.out.println("qBigramDistanceInDesc:"
				// + qBigramDistanceInDesc);
				System.out.println("median_relevance:" + medianRelevance);
				System.out.println("predict:" + predict);
				System.out.println("P1:" + P1);
				System.out.println("P2:" + P2);
				System.out.println("P3:" + P3);
				System.out.println("P4:" + P4);

				System.out.println("\n");
				// System.out.println();

			}
		} finally {
			trainIn.close();
		}

		System.out.flush();
	}

	public static void main(String[] args) throws Exception {
		args = new String[1];
		args[0] = "/home/markpeng/Share/Kaggle/Search Results Relevance/wrong_local_test_xgb_4498features_d=60_20150605.csv";

		WrongCaseViewer worker = new WrongCaseViewer();
		worker.view(args[0]);
	}
}

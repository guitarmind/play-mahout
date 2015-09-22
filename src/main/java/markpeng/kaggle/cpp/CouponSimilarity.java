package markpeng.kaggle.cpp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class CouponSimilarity {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private static final double MIN_SIMILARITY = 0.75;

	public List<String> readFile(String filePath) throws Exception {
		List<String> result = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				if (tmp.length() > 0 && !result.contains(tmp))
					result.add(tmp);
			}
		} finally {
			in.close();
		}

		return result;
	}

	public void run(String trainCouponListPath, String testCouponListPath)
			throws Exception {

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainCouponListPath), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testCouponListPath), "UTF-8"));

		CsvParserSettings settings = new CsvParserSettings();
		settings.setParseUnescapedQuotes(false);
		settings.getFormat().setLineSeparator("\n");
		settings.getFormat().setDelimiter(',');
		settings.getFormat().setQuote('"');
		settings.setHeaderExtractionEnabled(true);
		settings.setEmptyValue("");
		settings.setMaxCharsPerColumn(40960);

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
				String COUPON_ID_hash = tokens[23];

				double USABLE_DATE_MON = Double.parseDouble(tokens[14]);
				double USABLE_DATE_TUE = Double.parseDouble(tokens[15]);
				double USABLE_DATE_WEB = Double.parseDouble(tokens[16]);
				double USABLE_DATE_THU = Double.parseDouble(tokens[17]);
				double USABLE_DATE_FRI = Double.parseDouble(tokens[18]);
				double USABLE_DATE_SAT = Double.parseDouble(tokens[19]);
				double USABLE_DATE_SUN = Double.parseDouble(tokens[20]);
				double USABLE_DATE_HOLIDAY = Double.parseDouble(tokens[21]);
				double USABLE_DATE_BEFORE_HOLIDAY = Double
						.parseDouble(tokens[22]);

				double PRICE_RATE_Normalize = Double.parseDouble(tokens[29]);
				double CATALOG_PRICE_LOG = Double.parseDouble(tokens[30]);
				double DISCOUNT_PRICE_LOG = Double.parseDouble(tokens[31]);
				double DISPPERIOD_Normalize = Double.parseDouble(tokens[32]);
				double VALIDPERIOD_Normalize = Double.parseDouble(tokens[33]);
				double SUM_USABLE_DATE = Double.parseDouble(tokens[34]);

				System.out.println(COUPON_ID_hash);

				count++;
			}

			System.out.println("Total train records: " + count);
			// System.out
			// .println("Total query-matched records in title or description: "
			// + matched);
			// System.out
			// .println("Total not-matched records in title or description: "
			// + (count - matched));

		} finally {
			trainIn.close();
			testIn.close();
		}

	}

	public static void main(String[] args) throws Exception {
		String trainCouponListPath = "/home/markpeng/Share/Kaggle/cpp/coupon_list_train_en_markpeng.csv";
		String testCouponListPath = "/home/markpeng/Share/Kaggle/cpp/coupon_list_test_en_markpeng.csv";

		CouponSimilarity worker = new CouponSimilarity();
		worker.run(trainCouponListPath, testCouponListPath);
	}

}

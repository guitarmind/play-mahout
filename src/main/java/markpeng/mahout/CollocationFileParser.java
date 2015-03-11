package markpeng.mahout;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

public class CollocationFileParser {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private NgramCompoundUtil util = new NgramCompoundUtil();

	public void parse(String filePath, int scoreThreshold, int targetNgram,
			int topN) throws Exception {
		Hashtable<String, Double> table = new Hashtable<String, Double>();

		BufferedReader reader = null;
		try {
			System.out.println("\nParsing " + filePath
					+ " ... (Score Threshold: " + scoreThreshold + ")");

			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filePath), "UTF-8"));
			String aLine = null;
			while ((aLine = reader.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(aLine, ":");
				int count = tokenizer.countTokens();
				if (count == 4) {
					String key = "";
					double value = 0.0;

					int index = 0;
					while (tokenizer.hasMoreTokens()) {
						String token = tokenizer.nextToken();
						if (index == 1)
							key = token.trim();
						else if (index == 3)
							value = Double.parseDouble(token);

						index++;
					}

					if (value > 0 && value >= scoreThreshold) {
						// gathering ngrams to compound string
						// String compoundKey = util.gatherGrams(key);
						String compoundKey = key;

						StringTokenizer checker = new StringTokenizer(
								compoundKey);
						int gramNum = checker.countTokens();
						if (gramNum == targetNgram)
							table.put(compoundKey, value);
						// table.put(key, value);
					}
				} else
					throw new Exception("Cannot parse: " + aLine);
			}
		} finally {
			if (reader != null)
				reader.close();
		}

		// sort by value
		if (table.size() > 1) {
			File fileChecker = new File(filePath);
			String fileName = fileChecker.getName().substring(0,
					fileChecker.getName().lastIndexOf("."));
			fileName = fileName + "_sorted.txt";
			String outputPath = fileChecker.getAbsolutePath().replace(
					fileChecker.getName(), fileName);
			File fileDir = new File(fileName);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(fileDir, false), "UTF-8"));
			StringBuffer resultStr = new StringBuffer();

			try {

				HashtableSorting sorter = new HashtableSorting(table);
				List<String> sortedKeys = sorter
						.sorting(new HashtableSorting.DoubleComparator());
				int index = 1;
				for (String key : sortedKeys) {
					// System.out.println(index + ". " + key + ": " +
					// table.get(key));
					// output.append(index + ". " + key + ": " + table.get(key)
					// + newLine);
					if (index <= topN) {
						resultStr.append(key + ": " + table.get(key) + newLine);
						System.out.println(key + ": " + table.get(key)
								+ newLine);
					}

					if (resultStr.length() >= BUFFER_LENGTH) {
						out.write(resultStr.toString());
						out.flush();
						resultStr.setLength(0);
					}

					index++;
				}
			} finally {
				out.write(resultStr.toString());
				out.flush();
				out.close();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		CollocationFileParser parser = new CollocationFileParser();
		// String filePath =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/mmc_train_asm_4gram_colloc.txt";
		// parser.parse(
		// "/home/markpeng/Share/UitoxIR/CollocationResultDump/Subcategory/standard_3grams_result.txt",
		// 1, 3, true);
		// parser.parse(filePath, 1, 4, true);

		String filePath = args[0];
		int ngram = Integer.parseInt(args[1]);
		int topN = Integer.parseInt(args[2]);
		parser.parse(filePath, 1, ngram, topN);
	}
}

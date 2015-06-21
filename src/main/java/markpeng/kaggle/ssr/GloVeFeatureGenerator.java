package markpeng.kaggle.ssr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import markpeng.deep.learning.GloVeParser;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class GloVeFeatureGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private DecimalFormat fomatter = new DecimalFormat("#.########");

	private static final double MIN_SIMILARITY = 0.75;

	private static GloVeParser parser = new GloVeParser();

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

	public void generate(String trainFile, String testFile, String outputTrain,
			String outputTest, String glovePath, int vectorSize)
			throws Exception {

		// System.setOut(new PrintStream(
		// new BufferedOutputStream(
		// new FileOutputStream(
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/preprocess_notmatched_score4_20150518.txt")),
		// true));

		StringBuffer resultStr = new StringBuffer();

		TreeMap<String, double[]> gloveVectors = parser.parse(glovePath,
				vectorSize);

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
		resultStr.append("\"id\",");
		// query
		for (int i = 0; i < vectorSize; i++)
			resultStr.append("\"QVect_" + (i + 1) + "\",");
		// title
		for (int i = 0; i < vectorSize; i++)
			resultStr.append("\"TVect_" + (i + 1) + "\",");
		// desc
		for (int i = 0; i < vectorSize; i++)
			resultStr.append("\"DVect_" + (i + 1) + "\",");
		// (query, title) L2-distance
		resultStr.append("\"QT_L2distance\",");
		// (query, title) L1-distance
		resultStr.append("\"QT_L1distance\",");
		// (query, title) cosine similarity
		resultStr.append("\"QT_CosSim\",");
		// (query, desc) L2-distance
		resultStr.append("\"QD_L2distance\",");
		// (query, desc) L1-distance
		resultStr.append("\"QD_L1distance\",");
		// (query, desc) cosine similarity
		resultStr.append("\"QD_CosSim\"");

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

				// missing description: use title
				if (productDesc.length() < 3)
					productDesc = productTitle;

				resultStr.append("\"" + id + "\",");
				// query
				double[] queryVector = parser.getAverageVector(query,
						gloveVectors, vectorSize);
				for (int i = 0; i < vectorSize; i++) {
					if (Double.isInfinite(queryVector[i])
							|| Double.isNaN(queryVector[i]))
						resultStr.append("\"0\",");
					else
						resultStr.append("\"" + fomatter.format(queryVector[i])
								+ "\",");
				}
				// title
				double[] titleVector = parser.getAverageVector(productTitle,
						gloveVectors, vectorSize);
				for (int i = 0; i < vectorSize; i++) {
					if (Double.isInfinite(titleVector[i])
							|| Double.isNaN(titleVector[i]))
						resultStr.append("\"0\",");
					else
						resultStr.append("\"" + fomatter.format(titleVector[i])
								+ "\",");
				}
				// desc
				double[] descVector = parser.getAverageVector(productDesc,
						gloveVectors, vectorSize);
				for (int i = 0; i < vectorSize; i++) {
					if (Double.isInfinite(descVector[i])
							|| Double.isNaN(descVector[i]))
						resultStr.append("\"0\",");
					else
						resultStr.append("\"" + fomatter.format(descVector[i])
								+ "\",");
				}

				// (query, title) L2-distance
				double qtL2Distance = parser.vectorL2Distance(queryVector,
						titleVector);
				if (Double.isInfinite(qtL2Distance)
						|| Double.isNaN(qtL2Distance))
					resultStr.append("\"999\",");
				else
					resultStr.append("\"" + fomatter.format(qtL2Distance)
							+ "\",");
				// (query, title) L1-distance
				double qtL1Distance = parser.vectorL1Distance(queryVector,
						titleVector);
				if (Double.isInfinite(qtL1Distance)
						|| Double.isNaN(qtL1Distance))
					resultStr.append("\"999\",");
				else
					resultStr.append("\"" + fomatter.format(qtL1Distance)
							+ "\",");
				// (query, title) cosine similarity
				double qtCosSim = parser.vectorSimilarity(queryVector,
						titleVector);
				if (Double.isInfinite(qtCosSim) || Double.isNaN(qtCosSim))
					resultStr.append("\"0\",");
				else
					resultStr.append("\"" + fomatter.format(qtCosSim) + "\",");

				// (query, desc) L2-distance
				double qdL2Distance = parser.vectorL2Distance(queryVector,
						descVector);
				if (Double.isInfinite(qdL2Distance)
						|| Double.isNaN(qdL2Distance))
					resultStr.append("\"999\",");
				else
					resultStr.append("\"" + fomatter.format(qdL2Distance)
							+ "\",");
				// (query, desc) L1-distance
				double qdL1Distance = parser.vectorL1Distance(queryVector,
						descVector);
				if (Double.isInfinite(qdL1Distance)
						|| Double.isNaN(qdL1Distance))
					resultStr.append("\"999\",");
				else
					resultStr.append("\"" + fomatter.format(qdL1Distance)
							+ "\",");
				// (query, desc) cosine similarity
				double qdCosSim = parser.vectorSimilarity(queryVector,
						descVector);
				if (Double.isInfinite(qdCosSim) || Double.isNaN(qdCosSim))
					resultStr.append("\"0\"");
				else
					resultStr.append("\"" + fomatter.format(qdCosSim) + "\"");

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
		resultStr.append("\"id\",");
		// query
		for (int i = 0; i < vectorSize; i++)
			resultStr.append("\"QVect_" + (i + 1) + "\",");
		// title
		for (int i = 0; i < vectorSize; i++)
			resultStr.append("\"TVect_" + (i + 1) + "\",");
		// desc
		for (int i = 0; i < vectorSize; i++)
			resultStr.append("\"DVect_" + (i + 1) + "\",");
		// (query, title) L2-distance
		resultStr.append("\"QT_L2distance\",");
		// (query, title) L1-distance
		resultStr.append("\"QT_L1distance\",");
		// (query, title) cosine similarity
		resultStr.append("\"QT_CosSim\",");
		// (query, desc) L2-distance
		resultStr.append("\"QD_L2distance\",");
		// (query, desc) L1-distance
		resultStr.append("\"QD_L1distance\",");
		// (query, desc) cosine similarity
		resultStr.append("\"QD_CosSim\"");

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

				// missing description: use title
				if (productDesc.length() < 3)
					productDesc = productTitle;

				resultStr.append("\"" + id + "\",");
				// query
				double[] queryVector = parser.getAverageVector(query,
						gloveVectors, vectorSize);
				for (int i = 0; i < vectorSize; i++) {
					if (Double.isInfinite(queryVector[i])
							|| Double.isNaN(queryVector[i]))
						resultStr.append("\"0\",");
					else
						resultStr.append("\"" + fomatter.format(queryVector[i])
								+ "\",");
				}
				// title
				double[] titleVector = parser.getAverageVector(productTitle,
						gloveVectors, vectorSize);
				for (int i = 0; i < vectorSize; i++) {
					if (Double.isInfinite(titleVector[i])
							|| Double.isNaN(titleVector[i]))
						resultStr.append("\"0\",");
					else
						resultStr.append("\"" + fomatter.format(titleVector[i])
								+ "\",");
				}
				// desc
				double[] descVector = parser.getAverageVector(productDesc,
						gloveVectors, vectorSize);
				for (int i = 0; i < vectorSize; i++) {
					if (Double.isInfinite(descVector[i])
							|| Double.isNaN(descVector[i]))
						resultStr.append("\"0\",");
					else
						resultStr.append("\"" + fomatter.format(descVector[i])
								+ "\",");
				}

				// (query, title) L2-distance
				double qtL2Distance = parser.vectorL2Distance(queryVector,
						titleVector);
				if (Double.isInfinite(qtL2Distance)
						|| Double.isNaN(qtL2Distance))
					resultStr.append("\"999\",");
				else
					resultStr.append("\"" + fomatter.format(qtL2Distance)
							+ "\",");
				// (query, title) L1-distance
				double qtL1Distance = parser.vectorL1Distance(queryVector,
						titleVector);
				if (Double.isInfinite(qtL1Distance)
						|| Double.isNaN(qtL1Distance))
					resultStr.append("\"999\",");
				else
					resultStr.append("\"" + fomatter.format(qtL1Distance)
							+ "\",");
				// (query, title) cosine similarity
				double qtCosSim = parser.vectorSimilarity(queryVector,
						titleVector);
				if (Double.isInfinite(qtCosSim) || Double.isNaN(qtCosSim))
					resultStr.append("\"0\",");
				else
					resultStr.append("\"" + fomatter.format(qtCosSim) + "\",");

				// (query, desc) L2-distance
				double qdL2Distance = parser.vectorL2Distance(queryVector,
						descVector);
				if (Double.isInfinite(qdL2Distance)
						|| Double.isNaN(qdL2Distance))
					resultStr.append("\"999\",");
				else
					resultStr.append("\"" + fomatter.format(qdL2Distance)
							+ "\",");
				// (query, desc) L1-distance
				double qdL1Distance = parser.vectorL1Distance(queryVector,
						descVector);
				if (Double.isInfinite(qdL1Distance)
						|| Double.isNaN(qdL1Distance))
					resultStr.append("\"999\",");
				else
					resultStr.append("\"" + fomatter.format(qdL1Distance)
							+ "\",");
				// (query, desc) cosine similarity
				double qdCosSim = parser.vectorSimilarity(queryVector,
						descVector);
				if (Double.isInfinite(qdCosSim) || Double.isNaN(qdCosSim))
					resultStr.append("\"0\"");
				else
					resultStr.append("\"" + fomatter.format(qdCosSim) + "\"");

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

	public static void main(String[] args) throws Exception {
		args = new String[6];
		args[0] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train_filterred_porter_stem_compound_markpeng_20150606.csv";
		args[1] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test_filterred_porter_stem_compound_markpeng_20150606.csv";
		args[2] = "/home/markpeng/Share/Kaggle/Search Results Relevance/train_T+D_glove_markpeng_20150621.csv";
		args[3] = "/home/markpeng/Share/Kaggle/Search Results Relevance/test_T+D_glove_markpeng_20150621.csv";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/train_glove_markpeng_20150617.csv";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/test_glove_markpeng_20150617.csv";
		args[4] = "/home/markpeng/Share/Kaggle/Search Results Relevance/ssr_vectors_20150616.txt";

		if (args.length < 5) {
			System.out
					.println("Arguments: [train.csv] [test.csv] [output train] [output test] [glove file]");
			return;
		}
		String trainFile = args[0];
		String testFile = args[1];
		// String[] featureFiles = args[2].split("\\|");
		String outputTrain = args[2];
		String outputTest = args[3];
		String glovePath = args[4];

		GloVeFeatureGenerator worker = new GloVeFeatureGenerator();
		worker.generate(trainFile, testFile, outputTrain, outputTest,
				glovePath, 300);
	}
}

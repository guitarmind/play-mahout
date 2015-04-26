package markpeng.kaggle.tae;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class SentimentFeatureGenerator {

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

	public void generatePrefixAndSymbol(String trainFile, String testFile,
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

		// prefix with ':'
		List<String> prefixFeatures = prefixFeatureExtraction(trainFile,
				testFile);

		// create headers
		int p = 0;
		for (String prefix : prefixFeatures) {
			resultStr.append("Prefix_" + (p + 1) + ",");
			p++;
		}
		resultStr.append("Five_Who,");
		resultStr.append("Five_What,");
		resultStr.append("Five_Where,");
		resultStr.append("Five_When,");
		resultStr.append("Five_How,");
		resultStr.append("qmark,");
		resultStr.append("exmark,");
		resultStr.append("please,");
		resultStr.append("yearNumWithSemi,");
		resultStr.append("yearNum,");
		resultStr.append("recap,");
		resultStr.append("report,");
		resultStr.append("semimark,");
		resultStr.append("htokens,");
		resultStr.append("abtokens");
		resultStr.append(newLine);

		try {

			String aLine = null;
			// skip header
			trainIn.readLine();
			while ((aLine = trainIn.readLine()) != null) {
				int[] rowResult = new int[prefixFeatures.size() + 15];

				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String abst = tokens[5].replace("\"", "").trim();

				List<String> htokens = Arrays.asList(headline.split("\\s"));
				List<String> abtokens = Arrays.asList(abst.split("\\s"));

				// System.out.println(headline);

				int hlength = headline.length();
				if (headline.contains(":")
						&& headline.indexOf(":") <= (hlength / 2)) {
					String newFeature = headline.substring(0,
							headline.indexOf(":")).trim();

					int pIndex = 0;
					for (String t : prefixFeatures) {
						if (newFeature.equals(t))
							rowResult[pIndex] = 1;

						pIndex++;
					}
				}

				int otherStart = prefixFeatures.size();
				// who, what, where, when, how
				if (headline.contains("who"))
					rowResult[otherStart] = 1;
				if (headline.contains("what"))
					rowResult[otherStart + 1] = 1;
				if (headline.contains("where"))
					rowResult[otherStart + 2] = 1;
				if (headline.contains("when"))
					rowResult[otherStart + 3] = 1;
				if (headline.contains("how"))
					rowResult[otherStart + 4] = 1;

				// question mark
				if (headline.contains("?"))
					rowResult[otherStart + 5] = 1;

				// ! mark
				if (headline.contains("!"))
					rowResult[otherStart + 6] = 1;

				// please
				if (headline.contains("please"))
					rowResult[otherStart + 7] = 1;

				// contains a year number with a ':'
				if (headline.matches("[0-9]{4,4}:.*")) {
					rowResult[otherStart + 8] = 1;
					// System.out.println(headline + " ==> yearNumWithSemi");
				}

				// contains a year number
				if (headline.matches("[0-9]{4,4}.*")) {
					rowResult[otherStart + 9] = 1;
					// System.out.println(headline + " ==> yearNum");
				}

				// Recap
				if (headline.contains("recap"))
					rowResult[otherStart + 10] = 1;

				// Report
				if (headline.contains("report"))
					rowResult[otherStart + 11] = 1;

				// semicolon
				if (headline.contains(":"))
					rowResult[otherStart + 12] = 1;

				// header token size
				rowResult[otherStart + 13] = htokens.size();

				// abstract token size
				rowResult[otherStart + 14] = abtokens.size();

				// append output row
				int vIndex = 0;
				for (Integer v : rowResult) {
					if (vIndex < rowResult.length - 1)
						resultStr.append(v + ",");
					else
						resultStr.append(v + newLine);
					vIndex++;
				}

				if (resultStr.length() >= BUFFER_LENGTH) {
					trainOut.write(resultStr.toString());
					trainOut.flush();
					resultStr.setLength(0);
				}

			} // end of train file loop

		} finally {
			trainIn.close();

			trainOut.write(resultStr.toString());
			trainOut.flush();
			trainOut.close();
			resultStr.setLength(0);
		}

		// ---------------------------------------------------------------------------------------------------------------
		// test dataset

		try {
			resultStr.setLength(0);
			// create headers
			p = 0;
			for (String prefix : prefixFeatures) {
				resultStr.append("Prefix_" + (p + 1) + ",");
				p++;
			}
			resultStr.append("Five_Who,");
			resultStr.append("Five_What,");
			resultStr.append("Five_Where,");
			resultStr.append("Five_When,");
			resultStr.append("Five_How,");
			resultStr.append("qmark,");
			resultStr.append("exmark,");
			resultStr.append("please,");
			resultStr.append("yearNumWithSemi,");
			resultStr.append("yearNum,");
			resultStr.append("recap,");
			resultStr.append("report,");
			resultStr.append("semimark,");
			resultStr.append("htokens,");
			resultStr.append("abtokens");
			resultStr.append(newLine);

			String aLine = null;
			// skip header
			testIn.readLine();
			while ((aLine = testIn.readLine()) != null) {
				int[] rowResult = new int[prefixFeatures.size() + 15];

				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String abst = tokens[5].replace("\"", "").trim();

				List<String> htokens = Arrays.asList(headline.split("\\s"));
				List<String> abtokens = Arrays.asList(abst.split("\\s"));

				// System.out.println(headline);
				int hlength = headline.length();
				if (headline.contains(":")
						&& headline.indexOf(":") <= (hlength / 2)) {
					String newFeature = headline.substring(0,
							headline.indexOf(":")).trim();

					int pIndex = 0;
					for (String t : prefixFeatures) {
						if (newFeature.equals(t))
							rowResult[pIndex] = 1;

						pIndex++;
					}
				}

				int otherStart = prefixFeatures.size();
				// who, what, where, when, how
				if (headline.contains("who"))
					rowResult[otherStart] = 1;
				if (headline.contains("what"))
					rowResult[otherStart + 1] = 1;
				if (headline.contains("where"))
					rowResult[otherStart + 2] = 1;
				if (headline.contains("when"))
					rowResult[otherStart + 3] = 1;
				if (headline.contains("how"))
					rowResult[otherStart + 4] = 1;

				// question mark
				if (headline.contains("?"))
					rowResult[otherStart + 5] = 1;

				// ! mark
				if (headline.contains("!"))
					rowResult[otherStart + 6] = 1;

				// please
				if (headline.contains("please"))
					rowResult[otherStart + 7] = 1;

				// contains a year number with a ':'
				if (headline.matches("[0-9]{4,4}:.*")) {
					rowResult[otherStart + 8] = 1;
					System.out.println(headline + " ==> yearNumWithSemi");
				}

				// contains a year number
				if (headline.matches("[0-9]{4,4}.*")) {
					rowResult[otherStart + 9] = 1;
					System.out.println(headline + " ==> yearNum");
				}

				// Recap
				if (headline.contains("recap"))
					rowResult[otherStart + 10] = 1;

				// Report
				if (headline.contains("report"))
					rowResult[otherStart + 11] = 1;

				// semicolon
				if (headline.contains(":"))
					rowResult[otherStart + 12] = 1;

				// header token size
				rowResult[otherStart + 13] = htokens.size();

				// abstract token size
				rowResult[otherStart + 14] = abtokens.size();

				// append output row
				int vIndex = 0;
				for (Integer v : rowResult) {
					if (vIndex < rowResult.length - 1)
						resultStr.append(v + ",");
					else
						resultStr.append(v + newLine);
					vIndex++;
				}

				if (resultStr.length() >= BUFFER_LENGTH) {
					testOut.write(resultStr.toString());
					testOut.flush();
					resultStr.setLength(0);
				}

			} // end of train file loop

		} finally {
			testIn.close();

			testOut.write(resultStr.toString());
			testOut.flush();
			testOut.close();
			resultStr.setLength(0);
		}
	}

	public void generateSentiment(String trainFile, String testFile,
			String outputTrain, String outputTest, String... featureFiles)
			throws Exception {
		List<String> sentiments = readFeature(featureFiles);

		StringBuffer resultStr = new StringBuffer();

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testFile), "UTF-8"));

		BufferedWriter trainOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTrain, false), "UTF-8"));

		BufferedWriter testOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTest, false), "UTF-8"));

		// popular sentiments
		List<String> popSentiments = sentimentFeatureExtraction(trainFile,
				testFile, sentiments);

		// prefix with ':'
		List<String> prefixFeatures = prefixFeatureExtraction(trainFile,
				testFile);

		// create headers
		int i = 0;
		for (String senti : popSentiments) {
			resultStr.append("Senti_" + (i + 1) + ",");
			i++;
		}
		int p = 0;
		for (String prefix : prefixFeatures) {
			resultStr.append("Prefix_" + (p + 1) + ",");
			p++;
		}
		resultStr.append("Five_Who,");
		resultStr.append("Five_What,");
		resultStr.append("Five_Where,");
		resultStr.append("Five_When,");
		resultStr.append("Five_How,");
		resultStr.append("qmark,");
		resultStr.append("exmark,");
		resultStr.append("please,");
		resultStr.append("yearNumWithSemi,");
		resultStr.append("yearNum,");
		resultStr.append("recap,");
		resultStr.append("report,");
		resultStr.append("semimark,");
		resultStr.append("htokens,");
		resultStr.append("abtokens");
		resultStr.append(newLine);

		try {

			String aLine = null;
			// skip header
			trainIn.readLine();
			while ((aLine = trainIn.readLine()) != null) {
				int[] rowResult = new int[popSentiments.size()
						+ prefixFeatures.size() + 15];

				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String abst = tokens[5].replace("\"", "").trim();

				List<String> htokens = Arrays.asList(headline.split("\\s"));
				List<String> abtokens = Arrays.asList(abst.split("\\s"));

				int sIndex = 0;
				for (String s : popSentiments) {
					if (htokens.contains(s)) {
						// if (htokens.contains(s) || abtokens.contains(s)) {
						rowResult[sIndex] = 1;
						System.out.println(s + " ==> " + headline + "," + abst);
					}

					sIndex++;
				}

				// System.out.println(headline);

				int hlength = headline.length();
				if (headline.contains(":")
						&& headline.indexOf(":") <= (hlength / 2)) {
					String newFeature = headline.substring(0,
							headline.indexOf(":")).trim();

					int pIndex = popSentiments.size();
					for (String t : prefixFeatures) {
						if (newFeature.equals(t))
							rowResult[pIndex] = 1;

						pIndex++;
					}
				}

				int otherStart = popSentiments.size() + prefixFeatures.size();
				// who, what, where, when, how
				if (headline.contains("who"))
					rowResult[otherStart] = 1;
				if (headline.contains("what"))
					rowResult[otherStart + 1] = 1;
				if (headline.contains("where"))
					rowResult[otherStart + 2] = 1;
				if (headline.contains("when"))
					rowResult[otherStart + 3] = 1;
				if (headline.contains("how"))
					rowResult[otherStart + 4] = 1;

				// question mark
				if (headline.contains("?"))
					rowResult[otherStart + 5] = 1;

				// ! mark
				if (headline.contains("!"))
					rowResult[otherStart + 6] = 1;

				// please
				if (headline.contains("please"))
					rowResult[otherStart + 7] = 1;

				// contains a year number with a ':'
				if (headline.matches("[0-9]{4,4}:.*")) {
					rowResult[otherStart + 8] = 1;
					// System.out.println(headline + " ==> yearNumWithSemi");
				}

				// contains a year number
				if (headline.matches("[0-9]{4,4}.*")) {
					rowResult[otherStart + 9] = 1;
					// System.out.println(headline + " ==> yearNum");
				}

				// Recap
				if (headline.contains("recap"))
					rowResult[otherStart + 10] = 1;

				// Report
				if (headline.contains("report"))
					rowResult[otherStart + 11] = 1;

				// semicolon
				if (headline.contains(":"))
					rowResult[otherStart + 12] = 1;

				// header token size
				rowResult[otherStart + 13] = htokens.size();

				// abstract token size
				rowResult[otherStart + 14] = abtokens.size();

				// append output row
				int vIndex = 0;
				for (Integer v : rowResult) {
					if (vIndex < rowResult.length - 1)
						resultStr.append(v + ",");
					else
						resultStr.append(v + newLine);
					vIndex++;
				}

				if (resultStr.length() >= BUFFER_LENGTH) {
					trainOut.write(resultStr.toString());
					trainOut.flush();
					resultStr.setLength(0);
				}

			} // end of train file loop

		} finally {
			trainIn.close();

			trainOut.write(resultStr.toString());
			trainOut.flush();
			trainOut.close();
			resultStr.setLength(0);
		}

		// ---------------------------------------------------------------------------------------------------------------
		// test dataset

		try {
			resultStr.setLength(0);
			// create headers
			i = 0;
			for (String senti : popSentiments) {
				resultStr.append("Senti_" + (i + 1) + ",");
				i++;
			}
			p = 0;
			for (String prefix : prefixFeatures) {
				resultStr.append("Prefix_" + (p + 1) + ",");
				p++;
			}
			resultStr.append("Five_Who,");
			resultStr.append("Five_What,");
			resultStr.append("Five_Where,");
			resultStr.append("Five_When,");
			resultStr.append("Five_How,");
			resultStr.append("qmark,");
			resultStr.append("exmark,");
			resultStr.append("please,");
			resultStr.append("yearNumWithSemi,");
			resultStr.append("yearNum,");
			resultStr.append("recap,");
			resultStr.append("report,");
			resultStr.append("semimark,");
			resultStr.append("htokens,");
			resultStr.append("abtokens");
			resultStr.append(newLine);

			String aLine = null;
			// skip header
			testIn.readLine();
			while ((aLine = testIn.readLine()) != null) {
				int[] rowResult = new int[popSentiments.size()
						+ prefixFeatures.size() + 15];

				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String abst = tokens[5].replace("\"", "").trim();

				List<String> htokens = Arrays.asList(headline.split("\\s"));
				List<String> abtokens = Arrays.asList(abst.split("\\s"));

				int sIndex = 0;
				for (String s : popSentiments) {
					if (htokens.contains(s)) {
						// if (htokens.contains(s) || abtokens.contains(s)) {
						rowResult[sIndex] = 1;
						System.out.println(s + " ==> " + headline + "," + abst);
					}

					sIndex++;
				}

				// System.out.println(headline);
				int hlength = headline.length();
				if (headline.contains(":")
						&& headline.indexOf(":") <= (hlength / 2)) {
					String newFeature = headline.substring(0,
							headline.indexOf(":")).trim();

					int pIndex = popSentiments.size();
					for (String t : prefixFeatures) {
						if (newFeature.equals(t))
							rowResult[pIndex] = 1;

						pIndex++;
					}
				}

				int otherStart = popSentiments.size() + prefixFeatures.size();
				// who, what, where, when, how
				if (headline.contains("who"))
					rowResult[otherStart] = 1;
				if (headline.contains("what"))
					rowResult[otherStart + 1] = 1;
				if (headline.contains("where"))
					rowResult[otherStart + 2] = 1;
				if (headline.contains("when"))
					rowResult[otherStart + 3] = 1;
				if (headline.contains("how"))
					rowResult[otherStart + 4] = 1;

				// question mark
				if (headline.contains("?"))
					rowResult[otherStart + 5] = 1;

				// ! mark
				if (headline.contains("!"))
					rowResult[otherStart + 6] = 1;

				// please
				if (headline.contains("please"))
					rowResult[otherStart + 7] = 1;

				// contains a year number with a ':'
				if (headline.matches("[0-9]{4,4}:.*")) {
					rowResult[otherStart + 8] = 1;
					System.out.println(headline + " ==> yearNumWithSemi");
				}

				// contains a year number
				if (headline.matches("[0-9]{4,4}.*")) {
					rowResult[otherStart + 9] = 1;
					System.out.println(headline + " ==> yearNum");
				}

				// Recap
				if (headline.contains("recap"))
					rowResult[otherStart + 10] = 1;

				// Report
				if (headline.contains("report"))
					rowResult[otherStart + 11] = 1;

				// semicolon
				if (headline.contains(":"))
					rowResult[otherStart + 12] = 1;

				// header token size
				rowResult[otherStart + 13] = htokens.size();

				// abstract token size
				rowResult[otherStart + 14] = abtokens.size();

				// append output row
				int vIndex = 0;
				for (Integer v : rowResult) {
					if (vIndex < rowResult.length - 1)
						resultStr.append(v + ",");
					else
						resultStr.append(v + newLine);
					vIndex++;
				}

				if (resultStr.length() >= BUFFER_LENGTH) {
					testOut.write(resultStr.toString());
					testOut.flush();
					resultStr.setLength(0);
				}

			} // end of train file loop

		} finally {
			testIn.close();

			testOut.write(resultStr.toString());
			testOut.flush();
			testOut.close();
			resultStr.setLength(0);
		}
	}

	public List<String> sentimentFeatureExtraction(String trainFile,
			String testFile, List<String> sentiments) throws Exception {
		List<String> newFeatures = new ArrayList<String>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testFile), "UTF-8"));

		try {
			String aLine = null;
			// skip header
			in.readLine();
			Hashtable<String, Integer> sentiTable = new Hashtable<String, Integer>();
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String abst = tokens[5].replace("\"", "").trim();
				String popular = tokens[tokens.length - 2];

				List<String> htokens = Arrays.asList(headline.split("\\s"));
				// List<String> abtokens = Arrays.asList(abst.split("\\s"));

				int sIndex = 0;
				for (String s : sentiments) {
					// if ((htokens.contains(s) || abtokens.contains(s) &&
					// popular.equals("1"))
					if (htokens.contains(s)) {
						if (!sentiTable.containsKey(s))
							sentiTable.put(s, 1);
						else
							sentiTable.put(s, sentiTable.get(s) + 1);
					}

					sIndex++;
				}
			}

			int sentiCount = 0;
			for (String t : sentiTable.keySet()) {
				int freq = sentiTable.get(t);
				System.out.println(t + " ==> " + freq);
				sentiCount++;
			}

			System.out.println("Total detected sentiment features: "
					+ sentiCount);

			// make sure that test csv contains them
			aLine = null;
			// skip header
			in.readLine();
			while ((aLine = testIn.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String abst = tokens[5].replace("\"", "").trim();

				List<String> htokens = Arrays.asList(headline.split("\\s"));
				// List<String> abtokens = Arrays.asList(abst.split("\\s"));

				for (String t : sentiTable.keySet()) {
					if (htokens.contains(t)) {
						// if (htokens.contains(t) || abtokens.contains(t)) {
						if (!newFeatures.contains(t))
							newFeatures.add(t);
					}
				}

			}

			for (String t : newFeatures) {
				System.out.println(t);
			}
			System.out.println("Final feature count: " + newFeatures.size());
		} finally {
			in.close();
			testIn.close();
		}

		return newFeatures;
	}

	public List<String> prefixFeatureExtraction(String trainFile,
			String testFile) throws Exception {
		List<String> newFeatures = new ArrayList<String>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testFile), "UTF-8"));

		try {
			int symbolCount = 0;

			String aLine = null;
			// skip header
			in.readLine();
			Hashtable<String, Integer> symbolTable = new Hashtable<String, Integer>();
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String popular = tokens[tokens.length - 2];

				int hlength = headline.length();
				if (headline.contains(":")
						&& headline.indexOf(":") <= (hlength / 2)) {
					String newFeature = headline.substring(0,
							headline.indexOf(":")).trim();

					if (!symbolTable.containsKey(newFeature))
						symbolTable.put(newFeature, 1);
					else
						symbolTable.put(newFeature,
								symbolTable.get(newFeature) + 1);

					// System.out.println(newFeature + " ==> " + popular);
				}

			}

			for (String t : symbolTable.keySet()) {
				int freq = symbolTable.get(t);
				if (freq >= 2) {
					// System.out.println(t + " ==> " + freq);
					symbolCount++;
				}
			}

			// System.out
			// .println("Total detected symbol features: " + symbolCount);

			// make sure that test csv contains them
			aLine = null;
			// skip header
			in.readLine();
			while ((aLine = testIn.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();

				int hlength = headline.length();
				if (headline.contains(":")
						&& headline.indexOf(":") <= (hlength / 2)) {
					String newFeature = headline.substring(0,
							headline.indexOf(":")).trim();
					for (String t : symbolTable.keySet()) {
						if (newFeature.equals(t)) {
							if (!newFeatures.contains(newFeature))
								newFeatures.add(newFeature);
						}
					}
				}

			}

			for (String t : newFeatures)
				System.out.println(t);
			System.out.println("Final feature count: " + newFeatures.size());
		} finally {
			in.close();
			testIn.close();
		}

		return newFeatures;
	}

	public static void main(String[] args) throws Exception {
		args = new String[5];
		args[0] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/NYTimesBlogTrain.csv";
		args[1] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/NYTimesBlogTest.csv";
		args[2] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/sentiment/AFINN-111.txt";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/prefixSymbolTrain.csv";
		// args[4] =
		// "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/prefixSymbolTest.csv";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/specialTrain.csv";
		// args[4] =
		// "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/specialTest.csv";
		args[3] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/sentimentTrain.csv";
		args[4] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/sentimentTest.csv";

		if (args.length < 5) {
			System.out
					.println("Arguments: [train.csv] [test.csv] [featureFiles] [output train] [output test]");
			return;
		}
		String trainFile = args[0];
		String testFile = args[1];
		String[] featureFiles = args[2].split("\\|");
		String outputTrain = args[3];
		String outputTest = args[4];
		SentimentFeatureGenerator worker = new SentimentFeatureGenerator();
		// worker.generatePrefixAndSymbol(trainFile, testFile, outputTrain,
		// outputTest);
		worker.generateSentiment(trainFile, testFile, outputTrain, outputTest,
				featureFiles);
		// worker.prefixFeatureExtraction(trainFile, testFile);
		// List<String> features = worker.readFeature(featureFiles);
		// worker.sentimentFeatureExtraction(trainFile, testFile, features);

		// System.out.println("1914: russians dominate in east poland"
		// .matches("[0-9]{4,4}:.*"));
	}
}

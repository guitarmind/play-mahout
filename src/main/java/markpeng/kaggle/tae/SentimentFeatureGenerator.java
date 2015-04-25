package markpeng.kaggle.tae;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
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

	public void generate(String trainFile, String testFile, String outputCsv,
			String... featureFiles) throws Exception {
		List<String> features = readFeature(featureFiles);

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));
		try {
			int validN = 0;

			List<String> prefixFeatures = prefixFeatureExtraction(trainFile,
					testFile);

			// how, what, when, where, who

			// question mark

			// ! mark

			// please

			// contains a year number with a ':'

			// contains a year number

			// 'Daily Report:'

			// 'Morning Agenda:'

			// 'In Performance:'

			// 'Q. and A.:'

			// 'Pictures of the Day:'

			// 'Walkabout:'

			// 'New York Fashion Week:'

			// 'New York Today:'

			// ''The Newsroom' Recap:' => popular

			// Recap

			// Report

			// 'Ask Well:'

			System.out.println("Total # of sentiment features:" + validN);

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);
		}
	}

	public static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(
			Map<K, V> map) {
		SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
				new Comparator<Map.Entry<K, V>>() {
					@Override
					public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
						int res = e1.getValue().compareTo(e2.getValue());
						if (res > 0)
							return -1;
						if (res < 0)
							return 1;
						else
							return res;
					}
				});
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
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
		args = new String[4];
		args[0] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/NYTimesBlogTrain.csv";
		args[1] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/NYTimesBlogTest.csv";
		args[2] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/sentiment/AFINN-111.txt";
		args[3] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/sentimentTrainTest.csv";

		if (args.length < 4) {
			System.out
					.println("Arguments: [train.csv] [test.csv] [featureFiles] [output txt]");
			return;
		}
		String trainFile = args[0];
		String testFile = args[1];
		String[] featureFiles = args[2].split("\\|");
		String outputCsv = args[3];
		SentimentFeatureGenerator worker = new SentimentFeatureGenerator();
		// worker.generate(trainFile, testFile, outputCsv, featureFiles);
		worker.prefixFeatureExtraction(trainFile, testFile);

	}
}

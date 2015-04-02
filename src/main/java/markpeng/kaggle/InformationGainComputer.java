package markpeng.kaggle;

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
import java.util.TreeMap;
import java.util.TreeSet;

public class InformationGainComputer {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public static Hashtable<String, List<String>> readTrainLabel(
			String trainLabelFile) throws Exception {
		// <label, list<doc_ids>>
		Hashtable<String, List<String>> output = new Hashtable<String, List<String>>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainLabelFile), "UTF-8"));

		try {
			String aLine = null;
			// skip header line
			in.readLine();
			while ((aLine = in.readLine()) != null) {
				String[] sp = aLine.split(",");
				if (sp != null && sp.length > 0) {
					String fileName = sp[0].replaceAll("\"", "");
					String label = sp[1];

					// System.out.println(fileName + ", " + label);
					if (output.get(label) == null) {
						List<String> tmp = new ArrayList<String>();
						tmp.add(fileName);
						output.put(label, tmp);
					} else {
						List<String> tmp = output.get(label);
						tmp.add(fileName);
						output.put(label, tmp);
					}
				}
			}
		} finally {
			in.close();
		}

		return output;
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

	public static void main(String[] args) throws Exception {

		// System.out.println(Integer.MAX_VALUE - 5);

		// args = new String[4];
		// args[0] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/trainLabels.csv";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/train_bytes.csv";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/topN_infogain_20150327.txt";
		// args[3] = "500";

		if (args.length < 4) {
			System.out
					.println("Arguments: [label file] [ngram csv file] [output file] [topN]");
			return;
		}
		String trainLabelFile = args[0];
		String csvFile = args[1];
		String outputFile = args[2];
		int topN = Integer.parseInt(args[3]);
		int trainN = 10868;
		// int byteFeatureN = (int) Math.pow(256, 2);
		// System.out.println("Bytes feature size: " + byteFeatureN);

		StringBuffer outputStr = new StringBuffer();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFile, false), "UTF-8"));
		try {
			Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);
			double[] classSize = new double[9];
			double[] classProb = new double[9];
			for (int i = 0; i < 9; i++) {
				classSize[i] = labels.get(Integer.toString(i + 1)).size();
				classProb[i] = (double) classSize[i] / trainN;
			}

			// read csv file
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(csvFile), "UTF-8"));
			try {
				String aLine = null;
				// get header
				String header = in.readLine();
				String[] splitted = header.split(",");
				String[] featureNames = Arrays.copyOfRange(splitted, 1,
						splitted.length - 1);
				int featureN = featureNames.length;
				System.out.println("featureN: " + featureN);

				// count = 1 or 0
				int[] trueCount = new int[featureN];
				int[] falseCount = new int[featureN];
				int[][] classTrueCount = new int[featureN][9];
				int[][] classFalseCount = new int[featureN][9];
				TreeMap<Integer, Double> infoGainTable = new TreeMap<Integer, Double>();

				while ((aLine = in.readLine()) != null) {
					String[] tmp = aLine.trim().split(",");
					// String fileName = tmp[0];
					String raw = tmp[tmp.length - 1];
					if (raw.contains("class"))
						tmp[tmp.length - 1] = tmp[tmp.length - 1].substring(5);
					// System.out.println("class label: " + tmp[tmp.length -
					// 1]);

					int label = Integer.parseInt(tmp[tmp.length - 1]) - 1;
					int index = 0;
					for (int j = 1; j < tmp.length - 1; j++) {
						int value = Integer.parseInt(tmp[j]);
						// System.out.println("value:" + value);
						if (value > 0) {
							trueCount[index] = trueCount[index] + 1;
							classTrueCount[index][label] = classTrueCount[index][label] + 1;
						} else {
							falseCount[index] = falseCount[index] + 1;
							classFalseCount[index][label] = classFalseCount[index][label] + 1;
						}

						// System.out.println("trueCount[" + index + "]:"
						// + trueCount[index]);
						// System.out.println("classTrueCount[" + index + "]["
						// + label + "]:" + classTrueCount[index][label]);
						// System.out.println("falseCount[" + index + "]:"
						// + falseCount[index]);
						// System.out.println("classFalseCount[" + index + "]["
						// + label + "]:" + classFalseCount[index][label]);

						index++;
					}
				}

				// compute information gain
				for (int n = 0; n < featureN; n++) {
					double infoGain = 0.0;
					for (int i = 0; i < 2; i++) {
						if (i == 0) {
							double trueProb = (double) trueCount[n] / trainN;
							System.out.println("trueProb: " + trueProb);
							for (int j = 0; j < 9; j++) {
								double probVC = (double) classTrueCount[n][j]
										/ classSize[j];
								System.out.println("probVC: " + probVC);
								double value = probVC
										* Math.log((double) probVC
												/ (trueProb * classProb[j]));
								if (!Double.isInfinite(value)
										&& !Double.isNaN(value))
									infoGain += value;
							} // end of class loop
						} else {
							double falseProb = (double) falseCount[n] / trainN;
							System.out.println("falseProb: " + falseProb);
							for (int j = 0; j < 9; j++) {
								double probVC = (double) classFalseCount[n][j]
										/ classSize[j];
								System.out.println("probVC: " + probVC);
								double value = probVC
										* Math.log((double) probVC
												/ (falseProb * classProb[j]));

								if (!Double.isInfinite(value)
										&& !Double.isNaN(value))
									infoGain += value;
							} // end of class loop
						}
					} // end of value loop

					System.out.println("Completed feature " + n + ": "
							+ infoGain);

					infoGainTable.put(n, infoGain);
				} // end of ngram loop

				// get top-N features
				SortedSet<Map.Entry<Integer, Double>> sortedFeatures = entriesSortedByValues(infoGainTable);
				int validN = 0;
				for (Map.Entry<Integer, Double> m : sortedFeatures) {
					int index = m.getKey();
					double infoGain = m.getValue();

					if (!Double.isInfinite(infoGain) && !Double.isNaN(infoGain)) {
						if (validN < topN) {
							outputStr.append(featureNames[index] + ","
									+ infoGain);
							outputStr.append(newLine);

							System.out.println(featureNames[index] + ","
									+ infoGain);

							if (outputStr.length() >= BUFFER_LENGTH) {
								out.write(outputStr.toString());
								out.flush();
								outputStr.setLength(0);
							}

						} else
							break;

						validN++;
					}
				} // end of feature loop

				System.out.println("Total # of features: " + validN);
			} finally {
				in.close();
			}

		} finally {
			out.write(outputStr.toString());
			out.flush();
			out.close();
		}
	}
}

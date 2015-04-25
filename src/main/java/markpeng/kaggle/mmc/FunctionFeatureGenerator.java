package markpeng.kaggle.mmc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class FunctionFeatureGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public void generate(String trainFolder, String outputTxt, String fileType,
			int minDF) throws Exception {
		TreeMap<String, Integer> features = new TreeMap<String, Integer>();

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTxt, false), "UTF-8"));
		try {

			String folderName = trainFolder;
			List<String> fileList = new ArrayList<String>();
			for (final File fileEntry : (new File(trainFolder)).listFiles()) {
				if (fileEntry.getName().contains("." + fileType)) {
					String tmp = fileEntry.getName().substring(0,
							fileEntry.getName().lastIndexOf("."));
					fileList.add(tmp);
				}
			}

			for (String file : fileList) {
				File f = new File(folderName + "/" + file + "." + fileType);

				System.out.println("Loading " + f.getAbsolutePath());
				if (f.exists()) {
					List<String> func = new ArrayList<String>();
					String aLine = null;
					BufferedReader in = new BufferedReader(
							new InputStreamReader(new FileInputStream(
									f.getAbsolutePath()), "UTF-8"));
					while ((aLine = in.readLine()) != null) {
						String tmp = aLine.toLowerCase().trim();

						// extract function as feature
						String function = extractFunction(tmp);
						if (function != null && !func.contains(function)) {
							func.add(function);
							System.out
									.println("Detected function: " + function);
						}
					}
					in.close();

					// count DF
					for (String c : func) {
						if (features.containsKey(c))
							features.put(c, features.get(c) + 1);
						else
							features.put(c, 1);
					}

					System.out.println("Completed filtering file: " + file);
				}
			} // end of file loop

			// check if each feature exists
			SortedSet<Map.Entry<String, Integer>> sortedFeatures = entriesSortedByValues(features);
			int validN = 0;
			for (Map.Entry<String, Integer> m : sortedFeatures) {
				String feature = m.getKey();
				int df = m.getValue();
				if (df >= minDF && !feature.equals("copyright")) {
					resultStr.append(feature + "," + df + newLine);

					if (resultStr.length() >= BUFFER_LENGTH) {
						out.write(resultStr.toString());
						out.flush();
						resultStr.setLength(0);
					}

					validN++;
				}
			} // end of feature loop

			System.out.println("Total # of features (DF >= " + minDF + "): "
					+ validN);

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);
		}
	}

	private String extractFunction(String text) {
		String tmp = null;

		if (text.contains(";") && text.contains("(") && !text.contains(".text")) {
			int commentIndex = text.indexOf(";");
			int quoteIndex = text.indexOf("(");

			if (commentIndex < quoteIndex) {
				String[] arr = text.substring(0, quoteIndex).split("\\s");
				if (arr != null) {
					String candidate = arr[arr.length - 1];
					if (candidate.length() >= 3
							&& candidate.matches("[a-zA-Z]+[0-9]?"))
						tmp = candidate;
				}
			}
		}

		return tmp;
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

		if (args.length < 4) {
			System.out
					.println("Arguments: [train folder] [output txt] [file type] [minDF] ");
			return;
		}
		String trainFolder = args[0];
		String outputTxt = args[1];
		String fileType = args[2];
		int minDF = Integer.parseInt(args[3]);
		FunctionFeatureGenerator worker = new FunctionFeatureGenerator();
		worker.generate(trainFolder, outputTxt, fileType, minDF);

	}
}

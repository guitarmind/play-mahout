package markpeng.kaggle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrainingFileGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private static final int MAX_NGRAM = 2;
	private static final int MIN_DF = 2;
	private static final double MAX_DF_PERCENT = 0.85;

	public Hashtable<String, List<String>> readTrainLabel(String trainLabelFile)
			throws Exception {
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

	public List<String> readFeatureLabel(String featureFile) throws Exception {
		List<String> features = new ArrayList<String>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(featureFile), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.substring(0, aLine.indexOf(": ")).trim();
				if (!features.contains(tmp))
					features.add(tmp);
			}
		} finally {
			in.close();
		}

		return features;
	}

	public void generatCSV(String trainLabelFile, String trainFolder,
			String featureFile, String outputCsv) throws Exception {
		List<String> features = readFeatureLabel(featureFile);
		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));
		try {

			for (String label : labels.keySet()) {
				String folderName = trainFolder + "/" + label;
				List<String> fileList = labels.get(label);

				for (String file : fileList) {
					File f = new File(folderName + "/" + file
							+ ".bytes_filtered");
					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						StringBuffer fileContent = new StringBuffer();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();
							fileContent.append(tmp + " ");
						}
						in.close();
						String content = fileContent.toString();

						// add label
						resultStr.append(label + ",");

						// check if each feature exists
						int index = 0;
						for (String feature : features) {
							if (index < features.size() - 1) {
								if (content.contains(feature))
									resultStr.append("1,");
								else
									resultStr.append("0,");
							} else {
								if (content.contains(feature))
									resultStr.append("1" + newLine);
								else
									resultStr.append("0" + newLine);
							}

							index++;
						} // end of feature loop

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}

						System.out.println("Completed filtering file: " + file);
					}
				} // end of label file loop

			} // end of label loop

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
		}
	}

	public void generateLibsvm(String trainLabelFile, String trainFolder,
			String featureFile, String outputCsv) throws Exception {
		List<String> features = readFeatureLabel(featureFile);
		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));
		try {

			for (String label : labels.keySet()) {
				String folderName = trainFolder + "/" + label;
				List<String> fileList = labels.get(label);

				for (String file : fileList) {
					File f = new File(folderName + "/" + file
							+ ".bytes_filtered");
					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						StringBuffer fileContent = new StringBuffer();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();
							fileContent.append(tmp + " ");
						}
						in.close();
						String content = fileContent.toString();

						// add label
						resultStr.append(label + " ");

						// check if each feature exists
						int index = 0;
						for (String feature : features) {
							int termFreq = countTermFreq(feature, content);

							if (index < features.size() - 1) {
								if (content.contains(feature))
									resultStr.append(index + ":" + termFreq
											+ " ");
								// resultStr.append(index + ":1 ");
							} else {
								if (content.contains(feature))
									resultStr.append(index + ":" + termFreq
											+ " " + newLine);
								// resultStr.append(index + ":1 " + newLine);
							}

							index++;
						} // end of feature loop

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}

						System.out.println("Completed filtering file: " + file);
					}
				} // end of label file loop

			} // end of label loop

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
		}
	}

	private int countTermFreq(String word, String text) {
		Pattern pattern = Pattern.compile(word);
		Matcher matcher = pattern.matcher(text);
		int counter = 0;
		while (matcher.find())
			counter++;
		return counter;
	}

	public static void main(String[] args) throws Exception {

		// args = new String[5];
		// args[0] = "csv";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/trainLabels.csv";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/mmc_train_bytes_4gram_colloc_sorted.txt";
		// args[4] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/train_feature_boolvector.csv";

		if (args.length < 5) {
			System.out
					.println("Arguments: [model{csv|libsvm}] [train folder] [train label file] [feature file] [output csv]");
			return;
		}
		String mode = args[0];
		String trainFolder = args[1];
		String trainLabelFile = args[2];
		String featureFile = args[3];
		String outputCsv = args[4];
		TrainingFileGenerator worker = new TrainingFileGenerator();
		if (mode.equals("csv"))
			worker.generatCSV(trainLabelFile, trainFolder, featureFile,
					outputCsv);
		else if (mode.equals("libsvm"))
			worker.generateLibsvm(trainLabelFile, trainFolder, featureFile,
					outputCsv);

	}
}

package markpeng.kaggle.mmc;

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
import java.util.TreeSet;

public class FeatureBasedBytesTrainingFileGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public FeatureBasedBytesTrainingFileGenerator() {
	}

	public void generatCSV(String trainLabelFile, String folderName,
			String featureFile, String outputCsv, boolean filtered, int ngram)
			throws Exception {
		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));

		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		List<Long> features = readFeatures(featureFile);

		try {

			// add header line
			resultStr.append("fileName,");
			for (Long l : features) {
				resultStr.append(l + ",");

				if (resultStr.length() >= BUFFER_LENGTH) {
					out.write(resultStr.toString());
					out.flush();
					resultStr.setLength(0);
				}
			}
			resultStr.append("classLabel" + newLine);

			for (String label : labels.keySet()) {
				List<String> fileList = labels.get(label);

				for (String file : fileList) {
					File f = null;
					if (filtered)
						// f = new File(folderName + "/" + file
						// + ".bytes_filtered");
						f = new File(folderName + "/" + label + "/" + file
								+ ".bytes_filtered");
					else
						f = new File(folderName + "/" + file + ".bytes");

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						// add fileName
						resultStr.append(file + ",");

						TreeSet<Long> resultTable = new TreeSet<Long>();

						List<String> tokens = new ArrayList<String>();
						List<String> prevLastThreetokens = new ArrayList<String>();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\s");
							for (String token : sp) {
								if (!token.equals("??"))
									tokens.add(token);
							}

							// count byte ngram
							if (prevLastThreetokens.size() > 0)
								tokens.addAll(0, prevLastThreetokens);
							for (int i = 0; i < tokens.size(); i++) {
								int ngramEnd = i + (ngram - 1);

								if (i % ngram == 0 && ngramEnd < tokens.size()) {
									// String seq = tokens.get(i) + tokens.get(i
									// +
									// 1);
									String seq = "";
									for (int j = i; j <= ngramEnd; j++) {
										seq += tokens.get(j);
									}

									long code = Long.parseLong(seq, 16);
									resultTable.add(code);
								}
							}

							// keep last N-1 tokens
							if (tokens.size() > 0) {
								prevLastThreetokens.clear();
								for (int k = ngram - 1; k >= 1; k--)
									prevLastThreetokens.add(tokens.get(tokens
											.size() - k));
							}
							tokens.clear();
						}
						in.close();
						tokens.clear();
						prevLastThreetokens.clear();

						// write row data
						for (Long l : features) {
							if (resultTable.contains(l))
								resultStr.append("1,");
							else
								resultStr.append("0,");

							if (resultStr.length() >= BUFFER_LENGTH) {
								out.write(resultStr.toString());
								out.flush();
								resultStr.setLength(0);
							}
						}
						resultTable.clear();

						// add label
						resultStr.append(label + newLine);

						System.out.println("Completed filtering file: " + file);
					}

				} // end of file loop

				// System.out.println("Total # of detected " + ngram
				// + "-byte sequence: " + features.size());

			} // end of label loop

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);
		}
	}

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

	public static List<Long> readFeatures(String featureFile) throws Exception {
		List<Long> output = new ArrayList<Long>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(featureFile), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				if (aLine.contains(",")) {
					String[] sp = aLine.split(",");
					if (sp != null && sp.length > 0) {
						long feature = Long.parseLong(sp[0]);
						if (!output.contains(feature))
							output.add(feature);
					}
				} else {
					if (aLine.trim().length() > 0) {
						long feature = Long.parseLong(aLine.trim());
						output.add(feature);
					}
				}
			}
		} finally {
			in.close();
		}

		return output;
	}

	public static void main(String[] args) throws Exception {

		// args = new String[5];
		// args[0] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/trainLabels.csv";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/train_bytes.csv";
		// args[3] = "true";
		// args[4] = "2";

		if (args.length < 6) {
			System.out
					.println("Arguments: [train folder] [train label file] [feature file] [output csv] [filtered] [ngram]");
			return;
		}
		String trainFolder = args[0];
		String trainLabelFile = args[1];
		String featureFile = args[2];
		String outputCsv = args[3];
		boolean filterred = Boolean.parseBoolean(args[4]);
		int ngram = Integer.parseInt(args[5]);

		FeatureBasedBytesTrainingFileGenerator worker = new FeatureBasedBytesTrainingFileGenerator();
		worker.generatCSV(trainLabelFile, trainFolder, featureFile, outputCsv,
				filterred, ngram);

	}
}

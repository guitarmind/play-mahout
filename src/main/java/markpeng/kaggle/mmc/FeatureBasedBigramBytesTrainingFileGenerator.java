package markpeng.kaggle.mmc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class FeatureBasedBigramBytesTrainingFileGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public FeatureBasedBigramBytesTrainingFileGenerator() {
	}

	public void generatCSV(String trainLabelFile, String folderName,
			String featureFile, String outputCsv, boolean filtered, int ngram)
			throws Exception {
		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));

		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);
		List<Integer> features = readFeatures(featureFile);

		try {

			// add header line
			resultStr.append("fileName,");
			for (int i = 0; i < features.size(); i++) {
				resultStr.append(features.get(i) + ",");
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

						List<String> tokens = new ArrayList<String>();
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
						}
						in.close();

						// get doc frequency of n-byte sequence
						int[] table = new int[features.size()];
						for (int i = 0; i < tokens.size(); i++) {
							if (i % 2 == 0 && (i + 1) < tokens.size()) {
								String seq = tokens.get(i) + tokens.get(i + 1);
								int code = Integer.parseInt(seq, 16);

								if (features.contains(code))
									table[features.indexOf(code)] = 1;
							}
						}
						for (long l : table)
							resultStr.append(l + ",");

						tokens.clear();

						// add label
						resultStr.append(label + newLine);

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}

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

	public static List<Integer> readFeatures(String featureFile)
			throws Exception {
		List<Integer> output = new ArrayList<Integer>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(featureFile), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String[] sp = aLine.split(",");
				if (sp != null && sp.length > 0) {
					int feature = Integer.parseInt(sp[0]);
					if (!output.contains(feature))
						output.add(feature);
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

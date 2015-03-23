package markpeng.kaggle;

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

public class TrainingFileGenerator implements Runnable {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private static final int MAX_NGRAM = 2;
	private static final int MIN_DF = 2;
	private static final double MAX_DF_PERCENT = 0.85;

	String mode;
	String trainLabelFile;
	String trainFolder;
	String outputCsv;
	String fileType;
	boolean filtered;
	String[] featureFiles;

	public TrainingFileGenerator() {
	}

	public TrainingFileGenerator(String mode, String trainLabelFile,
			String trainFolder, String outputCsv, String fileType,
			boolean filtered, String... featureFiles) {
		this.mode = mode;
		this.trainLabelFile = trainLabelFile;
		this.trainFolder = trainFolder;
		this.outputCsv = outputCsv;
		this.fileType = fileType;
		this.filtered = filtered;
	}

	@Override
	public void run() {
		try {
			if (mode.equals("csv"))
				generatCSV(trainLabelFile, trainFolder, outputCsv, fileType,
						filtered, featureFiles);
			else if (mode.equals("libsvm"))
				generateLibsvm(trainLabelFile, trainFolder, outputCsv,
						fileType, filtered, featureFiles);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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

	public List<String> readFeature(String... featureFiles) throws Exception {
		List<String> features = new ArrayList<String>();

		for (String featureFile : featureFiles) {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(featureFile), "UTF-8"));

			try {
				String aLine = null;
				while ((aLine = in.readLine()) != null) {
					String tmp = aLine.toLowerCase().trim();
					if (tmp.length() > 0 && !features.contains(tmp))
						features.add(tmp);
				}
			} finally {
				in.close();
			}
		}

		// extra features
		if (!features.contains("db"))
			features.add("db");
		if (!features.contains("dd"))
			features.add("dd");

		return features;
	}

	public void generatCSV(String trainLabelFile, String trainFolder,
			String outputCsv, String fileType, boolean filtered,
			String... featureFiles) throws Exception {
		List<String> features = readFeature(featureFiles);
		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));
		try {

			// add header line
			int featureIndex = 0;
			resultStr.append("fileName,");
			for (String feature : features) {
				if (featureIndex < features.size() - 1)
					resultStr.append(feature + ",");
				else
					resultStr.append(feature + ",classLabel" + newLine);
				featureIndex++;
			}

			for (String label : labels.keySet()) {
				String folderName = trainFolder + "/" + label;
				List<String> fileList = labels.get(label);

				for (String file : fileList) {
					File f = null;
					if (filtered)
						f = new File(folderName + "/" + file + "." + fileType
								+ "_filtered");
					else
						f = new File(folderName + "/" + file + "." + fileType);

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						// add fileName
						resultStr.append(file + ",");

						StringBuffer fileContent = new StringBuffer();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\t{2,}\\s{2,}");
							List<String> tokens = Arrays.asList(sp);
							int index = 0;
							for (String token : tokens) {
								if (index > 0 && token.length() > 1) {
									fileContent.append(token + " ");
								}
								index++;
							}

							fileContent.append(newLine);
						}
						in.close();
						String content = fileContent.toString();
						fileContent.setLength(0);

						// get term frequency
						Hashtable<String, Integer> tfMap = getTermFreqByLucene(content);

						// check if each feature exists
						for (String feature : features) {
							// int termFreq = countTermFreqByRegEx(feature,
							// content);
							int termFreq = 0;
							if (tfMap.containsKey(feature))
								termFreq = tfMap.get(feature);

							resultStr.append(termFreq + ",");

						} // end of feature loop

						// add label
						resultStr.append(label + newLine);

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}

						System.out.println("Completed filtering file: " + file);
					}
				} // end of label file loop

			} // end of label loop

			System.out.println("Total # of features: " + features.size());

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);
		}
	}

	public void generateLibsvm(String trainLabelFile, String trainFolder,
			String outputCsv, String fileType, boolean filtered,
			String... featureFiles) throws Exception {
		List<String> features = readFeature(featureFiles);
		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));
		try {

			for (String label : labels.keySet()) {
				String folderName = trainFolder + "/" + label;
				List<String> fileList = labels.get(label);

				for (String file : fileList) {
					File f = null;
					if (filtered)
						f = new File(folderName + "/" + file + "." + fileType
								+ "_filtered");
					else
						f = new File(folderName + "/" + file + "." + fileType);

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						StringBuffer fileContent = new StringBuffer();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\t{2,}\\s{2,}");
							List<String> tokens = Arrays.asList(sp);
							int index = 0;
							for (String token : tokens) {
								if (index > 0 && token.length() > 1) {
									fileContent.append(token + " ");
								}
								index++;
							}

							fileContent.append(newLine);
						}
						in.close();
						String content = fileContent.toString();
						fileContent.setLength(0);

						// get term frequency
						Hashtable<String, Integer> tfMap = getTermFreqByLucene(content);

						// add label
						resultStr.append(label + " ");

						// check if each feature exists
						int index = 1;
						for (String feature : features) {
							// int termFreq = countTermFreqByRegEx(feature,
							// content);
							int termFreq = 0;
							if (tfMap.containsKey(feature)) {
								termFreq = tfMap.get(feature);
								resultStr.append(index + ":" + termFreq + " ");
							}

							index++;
						} // end of feature loop

						resultStr.append(newLine);

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}

						System.out.println("Completed filtering file: " + file);
					}
				} // end of label file loop

			} // end of label loop

			System.out.println("Total # of features: " + features.size());

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);
		}
	}

	private Hashtable<String, Integer> getTermFreqByLucene(String text)
			throws IOException {
		Hashtable<String, Integer> result = new Hashtable<String, Integer>();

		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					if (result.get(word) == null)
						result.put(word, 1);
					else {
						result.put(word, result.get(word) + 1);
					}

					wordCount++;
				}
			}

		} finally {
			// Fixed error : close ts:TokenStream
			ts.end();
			ts.close();
		}

		return result;
	}

	public static void main(String[] args) throws Exception {

		// args = new String[7];
		// args[0] = "csv";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/trainLabels.csv";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/newFeatures20150318.txt|"
		// +
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/rf_nonzero_features.txt";
		// args[4] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/submission.csv";
		// args[5] = "asm";
		// args[6] = "false";

		if (args.length < 6) {
			System.out
					.println("Arguments: [model{csv|libsvm}] [train folder] [train label file] [feature files] [output csv] [file type] [filtered]");
			return;
		}
		String mode = args[0];
		String trainFolder = args[1];
		String trainLabelFile = args[2];
		String[] featureFiles = args[3].split("\\|");
		String outputCsv = args[4];
		String fileType = args[5];
		boolean filterred = Boolean.parseBoolean(args[5]);
		TrainingFileGenerator worker = new TrainingFileGenerator();
		if (mode.equals("csv"))
			worker.generatCSV(trainLabelFile, trainFolder, outputCsv, fileType,
					filterred, featureFiles);
		else if (mode.equals("libsvm"))
			worker.generateLibsvm(trainLabelFile, trainFolder, outputCsv,
					fileType, filterred, featureFiles);

	}
}

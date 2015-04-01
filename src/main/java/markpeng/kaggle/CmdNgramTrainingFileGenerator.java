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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class CmdNgramTrainingFileGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private static final int MAX_NGRAM = 2;
	private static final int MIN_DF = 2;
	private static final double MAX_DF_PERCENT = 0.85;

	public CmdNgramTrainingFileGenerator() {
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
			String outputCsv, String fileType, boolean filtered, int ngram,
			String... featureFiles) throws Exception {

		List<String> features = readFeature(featureFiles);

		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));
		try {
			List<String> cmdNgramFeatures = new ArrayList<String>();
			Hashtable<String, Hashtable<String, Integer>> fileCmdNgramTable = new Hashtable<String, Hashtable<String, Integer>>();

			// first loop: get cmd ngram features
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
						List<String> lineCmds = new ArrayList<String>();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\t{2,}\\s{2,}");
							if (sp.length > 1) {
								String cmd = sp[1].trim();
								if (features.contains(cmd)) {
									lineCmds.add(cmd);
								}
							}
						}
						in.close();

						// create ngrams
						Hashtable<String, Integer> ngrams = getNgramFreqByLucene(
								lineCmds, ngram);
						for (String n : ngrams.keySet()) {
							if (!cmdNgramFeatures.contains(n))
								cmdNgramFeatures.add(n);
						}
						fileCmdNgramTable.put(file, ngrams);

						System.out.println("Completed filtering file: " + file);

					}
				} // end of label file loop

			} // end of label loop

			// add header line
			resultStr.append("fileName,");
			for (String feature : features) {
				resultStr.append(feature + ",");
			}
			int featureIndex = 0;
			for (String c : cmdNgramFeatures) {
				if (featureIndex < cmdNgramFeatures.size() - 1)
					resultStr.append(c.replace(" ", "_") + ",");
				else
					resultStr.append(c.replace(" ", "_") + ",classLabel"
							+ newLine);
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
						List<String> lineCmds = new ArrayList<String>();

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

								if (index == 1) {
									String cmd = sp[1].trim();
									if (features.contains(cmd)) {
										lineCmds.add(cmd);
									}
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
							if (tfMap.containsKey(feature)) {
								termFreq = tfMap.get(feature);
							}

							resultStr.append(termFreq + ",");

						} // end of feature loop

						// create ngrams
						Hashtable<String, Integer> ngrams = getNgramFreqByLucene(
								lineCmds, ngram);
						for (String c : cmdNgramFeatures) {
							int freq = 0;
							if (ngrams.containsKey(c))
								freq = ngrams.get(c);

							resultStr.append(freq + ",");
						}

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
			System.out.println("Total # of cmd line ngram features: "
					+ cmdNgramFeatures.size());

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

	private Hashtable<String, Integer> getNgramFreqByLucene(
			List<String> lineCmds, int ngram) throws IOException {
		Hashtable<String, Integer> result = new Hashtable<String, Integer>();

		StringBuffer text = new StringBuffer();
		for (String l : lineCmds)
			text.append(l + " ");

		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text.toString()));
		ts = new ShingleFilter(ts, ngram, ngram);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();
					if (word.split("\\s").length == ngram) {
						System.out.println(word);
						if (result.get(word) == null)
							result.put(word, 1);
						else {
							result.put(word, result.get(word) + 1);
						}
					}

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
		// args[0] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/trainLabels.csv";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/newFeatures20150318.txt|"
		// +
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/rf_nonzero_features.txt";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/submission.csv";
		// args[4] = "asm";
		// args[5] = "false";
		// args[6] = "2";

		if (args.length < 7) {
			System.out
					.println("Arguments: [train folder] [train label file] [feature files] [output csv] [file type] [filtered] [ngram]");
			return;
		}
		String trainFolder = args[0];
		String trainLabelFile = args[1];
		String[] featureFiles = args[2].split("\\|");
		String outputCsv = args[3];
		String fileType = args[4];
		boolean filterred = Boolean.parseBoolean(args[5]);
		int ngram = Integer.parseInt(args[6]);
		CmdNgramTrainingFileGenerator worker = new CmdNgramTrainingFileGenerator();
		worker.generatCSV(trainLabelFile, trainFolder, outputCsv, fileType,
				filterred, ngram, featureFiles);

	}
}

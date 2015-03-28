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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import javax.swing.text.TableView;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class BytesTrainingFileGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public BytesTrainingFileGenerator() {
	}

	public void generatCSV(String trainLabelFile, String folderName,
			String outputCsv, boolean filtered, int ngram) throws Exception {
		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));

		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		TreeSet<Long> tableKeys = new TreeSet<Long>();
		List<String> fileNames = new ArrayList<String>();
		Hashtable<String, HashSet<Long>> fileValues = new Hashtable<String, HashSet<Long>>();
		Hashtable<String, String> fileLabels = new Hashtable<String, String>();

		try {

			// List<String> targetLabels = new ArrayList<String>();
			// targetLabels.add("1");
			// targetLabels.add("4");
			// targetLabels.add("5");
			// targetLabels.add("6");
			// targetLabels.add("8");
			// targetLabels.add("9");
			// for (String label : targetLabels) {
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
						// long[] table = new long[65536];
						HashSet<Long> table = new HashSet<Long>();
						for (int i = 0; i < tokens.size(); i++) {
							int ngramEnd = i + (ngram - 1);

							if (i % ngram == 0 && ngramEnd < tokens.size()) {
								// String seq = tokens.get(i) + tokens.get(i +
								// 1);
								String seq = "";
								for (int j = i; j <= ngramEnd; j++) {
									seq += tokens.get(j);
								}

								long code = Long.parseLong(seq, 16);
								table.add(code);
								tableKeys.add(code);
							}
						}

						fileNames.add(file);
						fileValues.put(file, table);
						fileLabels.put(file, label);
						tokens.clear();

						System.out.println("Completed filtering file: " + file);
					}

				} // end of file loop

				// System.out.println("Total # of detected " + ngram
				// + "-byte sequence: " + features.size());

			} // end of label loop

			// add header line
			resultStr.append("fileName,");
			for (Long l : tableKeys) {
				resultStr.append(l + ",");

				if (resultStr.length() >= BUFFER_LENGTH) {
					out.write(resultStr.toString());
					out.flush();
					resultStr.setLength(0);
				}
			}
			resultStr.append("classLabel" + newLine);

			// generate final csv
			for (String file : fileNames) {
				// add fileName
				resultStr.append(file + ",");

				HashSet<Long> table = fileValues.get(file);
				for (Long l : tableKeys) {
					if (table.contains(l))
						resultStr.append("1,");
					else
						resultStr.append("0,");

					if (resultStr.length() >= BUFFER_LENGTH) {
						out.write(resultStr.toString());
						out.flush();
						resultStr.setLength(0);
					}
				}
				// resultStr.append(l + ",");

				// add label
				String label = fileLabels.get(file);
				resultStr.append(label + newLine);
				// resultStr.append("class" + label + newLine);

				System.out.println("Completed generating csv row for file: "
						+ file);
			}

			System.out.println("Total number of byte codes: "
					+ tableKeys.size());

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

		// args = new String[5];
		// args[0] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/trainLabels.csv";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/train_bytes.csv";
		// args[3] = "true";
		// args[4] = "2";

		if (args.length < 5) {
			System.out
					.println("Arguments: [train folder] [train label file] [output csv] [filtered] [ngram]");
			return;
		}
		String trainFolder = args[0];
		String trainLabelFile = args[1];
		String outputCsv = args[2];
		boolean filterred = Boolean.parseBoolean(args[3]);
		int ngram = Integer.parseInt(args[4]);

		// Hashtable<String, List<String>> labels =
		// readTrainLabel(trainLabelFile);

		BytesTrainingFileGenerator worker = new BytesTrainingFileGenerator();
		worker.generatCSV(trainLabelFile, trainFolder, outputCsv, filterred,
				ngram);

		// Thread[] threads = new Thread[labels.keySet().size()];
		// int i = 0;
		// for (String label : labels.keySet()) {
		// List<String> fileList = labels.get(label);
		//
		// System.out.println("Running for class " + label + " ...");
		// worker.generatCSV(label, fileList, trainFolder, outputCsv,
		// filterred, ngram);
		// // threads[i] = new Thread(worker);
		// // threads[i].start();
		//
		// // System.out.println("Running thread for class " + label + " ...");
		// // Thread.sleep(2000);
		//
		// i++;
		// }

		// for (Thread t : threads)
		// t.join();

	}
}

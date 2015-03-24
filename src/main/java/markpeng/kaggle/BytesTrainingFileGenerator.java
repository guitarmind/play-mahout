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

public class BytesTrainingFileGenerator implements Runnable {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	String label;
	List<String> trainFileNames;
	String trainFolder;
	String outputCsv;
	boolean filtered;
	int ngram;

	public BytesTrainingFileGenerator(String label,
			List<String> trainFileNames, String trainFolder, String outputCsv,
			boolean filtered, int ngram) {
		this.label = label;
		this.trainFileNames = trainFileNames;
		this.trainFolder = trainFolder;
		this.outputCsv = outputCsv;
		this.filtered = filtered;
		this.ngram = ngram;
	}

	@Override
	public void run() {
		try {
			generatCSV(label, trainFileNames, trainFolder, outputCsv, filtered,
					ngram);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void generatCSV(String label, List<String> fileList,
			String folderName, String outputCsv, boolean filtered, int ngram)
			throws Exception {
		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));
		try {

			// add header line
			resultStr.append("fileName,");
			for (int i = 0; i <= 65535; i++) {
				resultStr.append(i + ",");
			}
			resultStr.append("classLabel" + newLine);

			for (String file : fileList) {
				File f = null;
				if (filtered)
					f = new File(folderName + "/" + file + ".bytes_filtered");
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

					// get term frequency of n-byte sequence
					long[] table = new long[65536];

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
			} // end of label file loop

			// System.out.println("Total # of detected " + ngram
			// + "-byte sequence: " + features.size());

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

		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		// BytesTrainingFileGenerator worker = new BytesTrainingFileGenerator();
		// if (mode.equals("csv"))
		// worker.generatCSV(trainLabelFile, trainFolder, outputCsv, fileType,
		// filterred, featureFiles);
		// else if (mode.equals("libsvm"))
		// worker.generateLibsvm(trainLabelFile, trainFolder, outputCsv,
		// fileType, filterred, featureFiles);

	}
}

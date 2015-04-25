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

public class BytesTestingFileGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public BytesTestingFileGenerator() {
	}

	public void generatCSV(String testFolder, String outputCsv,
			boolean filtered, int ngram) throws Exception {
		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputCsv, false), "UTF-8"));

		try {

			File checker = new File(testFolder);
			if (checker.exists()) {
				// get all test file path
				List<String> testFiles = new ArrayList<String>();
				for (final File fileEntry : checker.listFiles()) {
					if (fileEntry.getName().contains(".bytes_filtered")) {
						String tmp = fileEntry.getAbsolutePath();
						testFiles.add(tmp);
					}
				}

				// add header line
				resultStr.append("fileName,");
				for (int i = 0; i <= 65535; i++) {
					if (i < 65535)
						resultStr.append(i + ",");
					else
						resultStr.append(i + newLine);
				}

				for (String file : testFiles) {
					File f = new File(file);
					String fileName = f.getName().trim();
					fileName = fileName.substring(0, fileName.lastIndexOf("."));

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						// add fileName
						resultStr.append(fileName + ",");

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
						long[] table = new long[65536];
						for (int i = 0; i < tokens.size(); i++) {
							if (i % 2 == 0 && (i + 1) < tokens.size()) {
								String seq = tokens.get(i) + tokens.get(i + 1);
								int code = Integer.parseInt(seq, 16);
								table[code] = 1;
							}
						}
						int index = 0;
						for (long l : table) {
							if (index < table.length - 1)
								resultStr.append(l + ",");
							else
								resultStr.append(l + newLine);

							index++;
						}

						tokens.clear();

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

		if (args.length < 4) {
			System.out
					.println("Arguments: [test folder] [output csv] [filtered] [ngram]");
			return;
		}
		String testFolder = args[0];
		String outputCsv = args[1];
		boolean filterred = Boolean.parseBoolean(args[2]);
		int ngram = Integer.parseInt(args[3]);

		BytesTestingFileGenerator worker = new BytesTestingFileGenerator();
		worker.generatCSV(testFolder, outputCsv, filterred, ngram);

	}
}
